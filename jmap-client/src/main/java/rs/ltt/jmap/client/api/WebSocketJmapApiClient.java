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
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.client.util.State;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.websocket.AbstractApiWebSocketMessage;
import rs.ltt.jmap.common.websocket.RequestWebSocketMessage;
import rs.ltt.jmap.common.websocket.WebSocketMessage;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.UUID;

public class WebSocketJmapApiClient extends AbstractJmapApiClient implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketJmapApiClient.class);

    private final HttpUrl webSocketUrl;
    private final HttpAuthentication authentication;
    private final ArrayList<JmapRequest> requestQueue = new ArrayList<>();
    private final HashMap<String, JmapRequest> inFlightRequests = new HashMap<>();
    private WebSocket currentWebSocket;
    private int attempt = 0;
    private State state = State.CLOSED;

    public WebSocketJmapApiClient(final HttpUrl webSocketUrl, final HttpAuthentication httpAuthentication, @Nullable final SessionStateListener sessionStateListener) {
        super(sessionStateListener);
        this.webSocketUrl = Preconditions.checkNotNull(webSocketUrl, "This WebSocket URL must not be null");
        this.authentication = httpAuthentication;
    }


    @Override
    public synchronized void execute(final JmapRequest jmapRequest) {
        if (readyToSend()) {
            send(jmapRequest);
        } else {
            requestQueue.add(jmapRequest);
        }
    }

    private void send(final JmapRequest jmapRequest) {
        final String requestId = UUID.randomUUID().toString();
        this.inFlightRequests.put(requestId, jmapRequest);
        final RequestWebSocketMessage message = RequestWebSocketMessage.builder()
                .requestId(requestId)
                .request(jmapRequest.getRequest())
                .build();
        requireWebSocket().send(Services.GSON.toJson(message));
    }

    private WebSocket requireWebSocket() {
        final WebSocket current = this.currentWebSocket;
        if (current == null) {
            throw new IllegalStateException(String.format(
                    "WebSocket was unexpectedly null even though we are in state %s", this.state)
            );
        }
        return current;
    }

    private synchronized boolean readyToSend() {
        if (state == State.CONNECTED) {
            return true;
        } else if (state.needsReconnect()) {
            transitionTo(State.CONNECTING);
            this.attempt++;
            startWebSocket();
            return false;
        } else if (state == State.CONNECTING) {
            return false;
        } else {
            throw new IllegalArgumentException(String.format("WebSocketClient is %s", this.state));
        }
    }

    private void transitionTo(final State state) {
        LOGGER.info("transition to {}", state);
        this.state = state;
    }

    private void startWebSocket() {
        LOGGER.info("Using WebSocket URL {}", this.webSocketUrl);
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(this.webSocketUrl);
        authentication.authenticate(requestBuilder);
        final Request request = requestBuilder.build();
        setCurrentWebSocket(Services.OK_HTTP_CLIENT.newWebSocket(request, new WebSocketProcessor(this)));
    }

    private void setCurrentWebSocket(final WebSocket webSocket) {
        if (this.currentWebSocket != null) {
            throw new IllegalStateException("Unable to set current WebSocket. One already exists");
        }
        this.currentWebSocket = webSocket;
    }

    @Override
    public boolean isValidFor(final Session session) {
        return true;
    }

    private void onMessage(final String text) {
        final WebSocketMessage message = Services.GSON.fromJson(text, WebSocketMessage.class);
        if (message instanceof AbstractApiWebSocketMessage) {
            onApiMessage((AbstractApiWebSocketMessage) message);
        }
    }

    protected void onApiMessage(final AbstractApiWebSocketMessage apiMessage) {
        final String requestId = apiMessage.getRequestId();
        final JmapRequest jmapRequest = inFlightRequests.remove(requestId);
        final Object payload = apiMessage.getPayload();
        if (payload instanceof GenericResponse) {
            processResponse(jmapRequest, (GenericResponse) payload);
        } else {
            //TODO close with 1007 (Invalid frame payload data)
            LOGGER.warn("Received WebSocketMessage of type {}", apiMessage.getClass().getSimpleName());
        }
    }

    private void onOpen() {
        attempt = 0;
        transitionTo(State.CONNECTED);
        final ListIterator<JmapRequest> iterator = requestQueue.listIterator();
        while (iterator.hasNext()) {
            final JmapRequest jmapRequest = iterator.next();
            this.send(jmapRequest);
            iterator.remove();
        }
    }

    private void onFailure(final Throwable throwable, final Response response) {
        transitionTo(State.FAILED);
        throwable.printStackTrace();
    }

    private void onClosed(final int code, final String reason) {
        transitionTo(State.CLOSED);
    }

    @Override
    public void close() throws IOException {
        final WebSocket webSocket = this.currentWebSocket;
        if (webSocket != null) {
            webSocket.cancel();
        }
    }

    private static class WebSocketProcessor extends WebSocketListener {

        private final WebSocketJmapApiClient client;

        private WebSocketProcessor(final WebSocketJmapApiClient client) {
            this.client = client;
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            client.onClosed(code, reason);
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            client.onFailure(t, response);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
            client.onMessage(text);
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);
            client.onOpen();
        }
    }
}
