/*
 * Copyright 2021 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rs.ltt.jmap.client.api;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.EOFException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.event.State;
import rs.ltt.jmap.client.http.Headers;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.websocket.AbstractApiWebSocketMessage;
import rs.ltt.jmap.common.websocket.RequestWebSocketMessage;
import rs.ltt.jmap.common.websocket.WebSocketMessage;

public class WebSocketJmapApiClient extends AbstractJmapApiClient implements Closeable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(WebSocketJmapApiClient.class);
    private static final String JMAP = "jmap";
    protected final List<Long> connectionDurations = new ArrayList<>();
    private final HttpUrl webSocketUrl;
    private final HttpAuthentication authentication;
    private final ArrayList<JmapRequest> requestQueue = new ArrayList<>();
    private final HashMap<String, JmapRequest> inFlightRequests = new HashMap<>();
    protected int attempt = 0;
    protected State state = State.CLOSED;
    protected ScheduledFuture<?> reconnectionFuture;
    private WebSocket currentWebSocket;
    private long lastFrameReceived = 0;

    public WebSocketJmapApiClient(
            final HttpUrl webSocketUrl,
            final HttpAuthentication httpAuthentication,
            @Nullable final SessionStateListener sessionStateListener) {
        super(sessionStateListener);
        this.webSocketUrl =
                Preconditions.checkNotNull(webSocketUrl, "This WebSocket URL must not be null");
        this.authentication = httpAuthentication;
    }

    @Override
    public synchronized void execute(final JmapRequest jmapRequest) {
        if (readyToSend()) {
            send(jmapRequest);
        } else {
            LOGGER.info("Queued up JmapRequest because not ready to send in state {}", this.state);
            requestQueue.add(jmapRequest);
        }
    }

    @Override
    public boolean isValidFor(final Session session) {
        return true;
    }

    private void send(final JmapRequest jmapRequest) {
        final String requestId = UUID.randomUUID().toString();
        this.inFlightRequests.put(requestId, jmapRequest);
        final RequestWebSocketMessage message =
                RequestWebSocketMessage.builder()
                        .id(requestId)
                        .request(jmapRequest.getRequest())
                        .build();
        if (send(message)) {
            return;
        }
        jmapRequest.setException(new Exception("Unable to send. WebSocket was closed"));
        this.inFlightRequests.remove(requestId);
    }

    protected boolean send(final WebSocketMessage message) {
        if (Services.OK_HTTP_LOGGER.isDebugEnabled()) {
            Services.OK_HTTP_LOGGER.debug("--> {}", Services.GSON.toJson(message));
        }
        return requireWebSocket().send(Services.GSON.toJson(message));
    }

    private WebSocket requireWebSocket() {
        final WebSocket current = this.currentWebSocket;
        if (current == null) {
            throw new IllegalStateException(
                    String.format(
                            "WebSocket was unexpectedly null even though we are in state %s",
                            this.state));
        }
        return current;
    }

    protected boolean readyToSend() {
        if (state == State.CONNECTED) {
            return true;
        } else if (state.needsReconnect()) {
            connectWebSocket();
            return false;
        } else if (state == State.CONNECTING) {
            return false;
        } else {
            throw new IllegalArgumentException(String.format("WebSocketClient is %s", this.state));
        }
    }

    protected void connectWebSocket() {
        this.attempt++;
        cancelReconnectionFuture();
        transitionTo(State.CONNECTING);
        startWebSocket();
    }

    protected void transitionTo(final State state) {
        LOGGER.info("transition to {}", state);
        this.state = state;
    }

    private void cancelReconnectionFuture() {
        final ScheduledFuture<?> future = this.reconnectionFuture;
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private void startWebSocket() {
        LOGGER.info("Using WebSocket URL {}", this.webSocketUrl);
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(this.webSocketUrl);
        authentication.authenticate(requestBuilder);
        requestBuilder.header(Headers.SEC_WEB_SOCKET_PROTOCOL, JMAP);
        final Request request = requestBuilder.build();
        final OkHttpClient okHttpClient =
                Services.OK_HTTP_CLIENT
                        .newBuilder()
                        .callTimeout(30, TimeUnit.SECONDS)
                        .pingInterval(getPingInterval())
                        .build();
        setCurrentWebSocket(okHttpClient.newWebSocket(request, new WebSocketProcessor(this)));
    }

    protected Duration getPingInterval() {
        return Duration.ZERO;
    }

    private void setCurrentWebSocket(final WebSocket webSocket) {
        if (this.currentWebSocket != null) {
            throw new IllegalStateException("Unable to set current WebSocket. One already exists");
        }
        this.currentWebSocket = webSocket;
    }

    private synchronized void onMessage(final WebSocket webSocket, final String text) {
        this.lastFrameReceived = System.nanoTime();
        if (Services.OK_HTTP_LOGGER.isDebugEnabled()) {
            Services.OK_HTTP_LOGGER.debug("<-- {}", text);
        }
        final WebSocketMessage message;
        try {
            message = Services.GSON.fromJson(text, WebSocketMessage.class);
        } catch (final Exception e) {
            LOGGER.error("Unable to parse incoming WebSocketMessage", e);
            // If a client receives a message that is not in the form of a JSON Problem Details
            // object,
            // a JMAP Response object, or a JMAP StateChange object, the client can either ignore
            // the message
            // or close the WebSocket connection. In the latter case, the endpoint MAY send a Close
            // frame with
            // a status code of 1007 (Invalid frame payload data),

            // we rather fail our pending requests instead of missing a response and never calling
            // the future
            policyViolation(e);
            return;
        }
        onWebSocketMessage(message);
    }

    protected boolean onWebSocketMessage(final WebSocketMessage message) {
        if (message instanceof AbstractApiWebSocketMessage) {
            return onApiMessage((AbstractApiWebSocketMessage) message);
        }
        return false;
    }

    protected boolean onApiMessage(final AbstractApiWebSocketMessage apiMessage) {
        final String requestId = apiMessage.getRequestId();
        if (requestId == null) {
            // all our Requests have an id set. We expect the server to do the same
            policyViolation(
                    new IllegalStateException(
                            String.format(
                                    "Server sent %s w/o requestId",
                                    apiMessage.getClass().getSimpleName())));
            return false;
        }
        final JmapRequest jmapRequest = inFlightRequests.remove(requestId);
        if (jmapRequest == null) {
            policyViolation(
                    new IllegalStateException(
                            String.format(
                                    "Could not find in flight request with id %s", requestId)));
            return false;
        }
        final Object payload = apiMessage.getPayload();
        if (payload instanceof GenericResponse) {
            processResponse(jmapRequest, (GenericResponse) payload);
            return false;
        }
        return false;
    }

    private void disconnect(final State state) {
        final WebSocket currentWebSocket = this.currentWebSocket;
        if (currentWebSocket != null) {
            currentWebSocket.cancel();
            this.currentWebSocket = null;
            transitionTo(state);
        }
    }

    private void policyViolation(final Throwable throwable) {
        disconnect(State.FAILED);
        failPendingRequests(throwable);
    }

    private void failPendingRequests(final Throwable throwable) {
        failPendingRequests(requestQueue.listIterator(), throwable);
        failPendingRequests(inFlightRequests.values().iterator(), throwable);
    }

    private static void failPendingRequests(
            Iterator<JmapRequest> iterator, final Throwable throwable) {
        while (iterator.hasNext()) {
            final JmapRequest jmapRequest = iterator.next();
            jmapRequest.setException(throwable);
            iterator.remove();
        }
    }

    protected synchronized void onOpen() {
        this.attempt = 0;
        transitionTo(State.CONNECTED);
        this.lastFrameReceived = System.nanoTime();
        final ListIterator<JmapRequest> iterator = requestQueue.listIterator();
        while (iterator.hasNext()) {
            final JmapRequest jmapRequest = iterator.next();
            this.send(jmapRequest);
            iterator.remove();
        }
    }

    private synchronized void onFailure(final Throwable throwable, final Response response) {
        final boolean showFailure = state != State.FAILED;
        final boolean wasConnected = state == State.CONNECTED;
        disconnect(State.FAILED);
        if (showFailure) {
            LOGGER.info("Unable to connect to WebSocket URL", throwable);
        }
        if (throwable instanceof EOFException && wasConnected) {
            this.connectionDurations.add(System.nanoTime() - lastFrameReceived);
        }
        failPendingRequests(throwable);
    }

    private void onClosing(WebSocket webSocket, int code, String reason) {
        LOGGER.info("Server closed the connection with code {} and reason {}", code, reason);
        disconnect(State.CLOSED);
        failPendingRequests(new WebSocketClosedException(code, reason));
    }

    @Override
    public synchronized void close() {
        disconnect(State.CLOSED);
        cancelReconnectionFuture();
    }

    private static class WebSocketProcessor extends WebSocketListener {

        private final WebSocketJmapApiClient client;

        private WebSocketProcessor(final WebSocketJmapApiClient client) {
            this.client = client;
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
            client.onClosing(webSocket, code, reason);
            ;
        }

        @Override
        public void onFailure(
                @NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            client.onFailure(t, response);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
            client.onMessage(webSocket, text);
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            client.onOpen();
        }
    }
}
