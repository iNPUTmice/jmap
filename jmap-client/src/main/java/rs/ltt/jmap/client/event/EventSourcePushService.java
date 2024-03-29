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

package rs.ltt.jmap.client.event;

import static rs.ltt.jmap.client.Services.GSON;
import static rs.ltt.jmap.client.Services.OK_HTTP_CLIENT;

import com.google.common.base.Strings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.http.Headers;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.StateChange;

public class EventSourcePushService implements PushService, OnStateChangeListenerManager.Callback {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourcePushService.class);

    private final Session session;
    private final HttpAuthentication authentication;
    private final OnStateChangeListenerManager onStateChangeListenerManager =
            new OnStateChangeListenerManager(this);
    private final List<OnConnectionStateChangeListener> onConnectionStateListeners =
            new ArrayList<>();
    private EventSource currentEventSource;
    private Duration pingInterval = Duration.ofSeconds(30);
    private ReconnectionStrategy reconnectionStrategy =
            ReconnectionStrategy.truncatedBinaryExponentialBackoffStrategy(60, 4);
    private int attempt = 0;
    private State state = State.CLOSED;
    private ScheduledFuture<?> reconnectionFuture;

    // TODO do we want to pass JmapClient instead to always have access to the session. or an
    // SessionRetriever interface
    public EventSourcePushService(final Session session, final HttpAuthentication authentication) {
        this.session = session;
        this.authentication = authentication;
    }

    private void disconnect(final State state) {
        final EventSource currentEventSource = this.currentEventSource;
        if (currentEventSource != null) {
            currentEventSource.cancel();
        }
        this.currentEventSource = null;
        transitionTo(state);
    }

    private void transitionTo(final State state) {
        LOGGER.info("transition to {}", state);
        this.state = state;
        synchronized (this.onConnectionStateListeners) {
            for (OnConnectionStateChangeListener listener : this.onConnectionStateListeners) {
                listener.onConnectionStateChange(state);
            }
        }
        if (state.needsReconnect()
                && this.onStateChangeListenerManager.isPushNotificationsEnabled()) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        final int attempt = this.attempt;
        final Duration reconnectIn = reconnectionStrategy.getNextReconnectionAttempt(attempt);
        LOGGER.info("schedule reconnect in {} for {} time ", reconnectIn, attempt + 1);
        this.reconnectionFuture =
                Services.SCHEDULED_EXECUTOR_SERVICE.schedule(
                        this::connect, reconnectIn.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void cancelReconnectionFuture() {
        final ScheduledFuture<?> future = this.reconnectionFuture;
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private void connect() {
        // TODO there needs to be some synchronization since connect can be called externally
        if (!this.state.needsReconnect()) {
            return;
        }
        this.attempt++;
        cancelReconnectionFuture();
        transitionTo(State.CONNECTING);

        final HttpUrl eventSourceUrl;
        try {
            eventSourceUrl =
                    session.getEventSourceUrl(
                            Collections.emptyList(), CloseAfter.NO, pingInterval.getSeconds());
        } catch (final Exception e) {
            LOGGER.warn("Unable to connect to EventSource URL");
            disconnect(State.FAILED);
            return;
        }
        connectEventSource(eventSourceUrl);
    }

    private void connectEventSource(final HttpUrl eventSourceUrl) {
        final EventSource.Factory factory =
                EventSources.createFactory(
                        OK_HTTP_CLIENT
                                .newBuilder()
                                .readTimeout(pingInterval.plus(PING_INTERVAL_TOLERANCE))
                                .retryOnConnectionFailure(true)
                                .build());
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(eventSourceUrl);
        authentication.authenticate(requestBuilder);
        // Something in the Cyrus-Nginx-OkHttp pipeline doesn't support compression
        // Since curl can handle it fine it might be OkHttp
        // Okio.GzipSource throws in checkEqual
        requestBuilder.addHeader(Headers.ACCEPT_ENCODING, "identity");
        final Request request = requestBuilder.build();
        LOGGER.info("Using event source url {}", eventSourceUrl);
        setCurrentEventSource(factory.newEventSource(request, new EventSourceProcessor()));
    }

    private void setCurrentEventSource(final EventSource eventSource) {
        if (this.currentEventSource != null) {
            throw new IllegalStateException(
                    "Unable to set current EventSource. One already exists");
        }
        this.currentEventSource = eventSource;
    }

    @Override
    public void setPingInterval(final Duration pingInterval) {
        this.pingInterval = pingInterval;
    }

    public void setReconnectionStrategy(final ReconnectionStrategy reconnectionStrategy) {
        this.reconnectionStrategy = reconnectionStrategy;
    }

    private void onStateEvent(final String id, final String state) {
        final StateChange stateChange = GSON.fromJson(state, StateChange.class);
        this.onStateChangeListenerManager.onStateChange(stateChange);
    }

    private void onPingEvent() {
        LOGGER.info("ping event received");
    }

    @Override
    public void addOnStateChangeListener(final OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListenerManager.addOnStateChangeListener(onStateChangeListener);
    }

    @Override
    public void removeOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListenerManager.removeOnStateChangeListener(onStateChangeListener);
    }

    @Override
    public void addOnConnectionStateListener(
            OnConnectionStateChangeListener onConnectionStateListener) {
        synchronized (this.onConnectionStateListeners) {
            this.onConnectionStateListeners.add(onConnectionStateListener);
        }
    }

    @Override
    public void removeOnConnectionStateListener(
            OnConnectionStateChangeListener onConnectionStateListener) {
        synchronized (this.onConnectionStateListeners) {
            this.onConnectionStateListeners.remove(onConnectionStateListener);
        }
    }

    @Override
    public State getConnectionState() {
        return this.state;
    }

    @Override
    public void disable() {
        disconnect(State.CLOSED);
        cancelReconnectionFuture();
    }

    @Override
    public void enable() {
        connect();
    }

    private static final class Type {
        public static final String STATE = "state";
        public static final String PING = "ping";
    }

    private class EventSourceProcessor extends EventSourceListener {
        @Override
        public void onClosed(@NotNull EventSource eventSource) {
            super.onClosed(eventSource);
            disconnect(State.CLOSED);
        }

        @Override
        public void onEvent(
                @NotNull EventSource eventSource,
                @Nullable String id,
                @Nullable String type,
                @NotNull String data) {
            super.onEvent(eventSource, id, type, data);
            switch (Strings.nullToEmpty(type)) {
                case Type.STATE:
                    onStateEvent(id, data);
                    break;
                case Type.PING:
                    onPingEvent();
                    break;
                default:
            }
        }

        @Override
        public void onFailure(
                @NotNull EventSource eventSource,
                @Nullable Throwable t,
                @Nullable Response response) {
            super.onFailure(eventSource, t, response);
            if (onStateChangeListenerManager.isPushNotificationsEnabled()) {
                if (t != null) {
                    LOGGER.warn("Unable to connect to EventSource URL", t);
                } else if (response != null) {
                    LOGGER.warn(
                            "Unable to connect to EventSource URL. Status code was {}",
                            response.code());
                } else {
                    LOGGER.warn("Unable to connect to EventSource URL");
                }
                disconnect(State.FAILED);
            }
        }

        @Override
        public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
            super.onOpen(eventSource, response);
            attempt = 0;
            transitionTo(State.CONNECTED);
        }
    }
}
