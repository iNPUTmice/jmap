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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.StateChange;
import rs.ltt.jmap.gson.JmapAdapters;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class EventSourcePushService implements PushService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourcePushService.class);

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final List<State> STATES_NEEDING_RECONNECT = Arrays.asList(State.CLOSED, State.FAILED);
    private final Session session;
    private final HttpAuthentication authentication;
    private OnStateChangeListener onStateChangeListener;
    private EventSource currentEventSource;
    private Duration pingInterval = Duration.ofSeconds(30);
    private Duration pingIntervalTolerance = Duration.ofSeconds(10);
    private ReconnectionStrategy reconnectionStrategy = ReconnectionStrategy.truncatedBinaryExponentialBackoffStrategy(60, 4);
    private int attempt = 0;
    private State state = State.CLOSED;
    private ScheduledFuture<?> reconnectionFuture;

    public EventSourcePushService(final Session session, final HttpAuthentication authentication) {
        this.session = session;
        this.authentication = authentication;
    }

    public void disconnect() {
        disconnect(State.DISCONNECTED);
        cancelReconnectionFuture();
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
        if (STATES_NEEDING_RECONNECT.contains(state)) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        final Duration reconnectIn = reconnectionStrategy.getNextReconnectionAttempt(attempt);
        LOGGER.info("schedule reconnect in {}", reconnectIn);
        this.reconnectionFuture = SCHEDULED_EXECUTOR_SERVICE.schedule(
                this::connect,
                reconnectIn.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void cancelReconnectionFuture() {
        final ScheduledFuture<?> future = this.reconnectionFuture;
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    public void connect() {
        if (!STATES_NEEDING_RECONNECT.contains(this.state)) {
            return;
        }
        this.attempt++;
        cancelReconnectionFuture();
        transitionTo(State.CONNECTING);
        final EventSource.Factory factory = EventSources.createFactory(
                new OkHttpClient.Builder()
                        .readTimeout(pingInterval.plus(pingIntervalTolerance))
                        .retryOnConnectionFailure(false)
                        .build()
        );
        final HttpUrl eventSourceUrl = session.getEventSourceUrl(
                Collections.emptyList(),
                CloseAfter.NO,
                pingInterval.getSeconds()
        );
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(eventSourceUrl);
        authentication.authenticate(requestBuilder);
        final Request request = requestBuilder.build();
        LOGGER.info("Using event source url {}", eventSourceUrl);
        setCurrentEventSource(factory.newEventSource(request, new EventSourceProcessor()));
    }

    private void setCurrentEventSource(final EventSource eventSource) {
        if (this.currentEventSource != null) {
            throw new IllegalStateException("Unable to set current EventSource. One already exists");
        }
        this.currentEventSource = eventSource;
    }

    public void setPingInterval(final Duration pingInterval) {
        this.pingInterval = pingInterval;
    }

    public void setPingIntervalTolerance(final Duration pingIntervalTolerance) {
        this.pingIntervalTolerance = pingIntervalTolerance;
    }

    public void setReconnectionStrategy(final ReconnectionStrategy reconnectionStrategy) {
        this.reconnectionStrategy = reconnectionStrategy;
    }

    private void onStateEvent(final String id, final String state) {
        //TODO externalize the gson into a static final something (it's thread safe anyway)
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        final Gson gson = gsonBuilder.create();
        final StateChange stateChange = gson.fromJson(state, StateChange.class);
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChange(stateChange);
        }
    }

    private void onPingEvent() {
        LOGGER.info("ping event received");
    }

    @Override
    public void setOnStateChangeListener(final OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    private enum State {
        CLOSED, CONNECTING, CONNECTED, FAILED, DISCONNECTED
    }

    private static final class Type {
        public static final String STATE = "state";
        public static final String PING = "ping";
    }

    private class EventSourceProcessor extends EventSourceListener {
        @Override
        public void onClosed(@NotNull EventSource eventSource) {
            super.onClosed(eventSource);
            //TODO if CloseAfter.State we should probably reconnect immediately
            disconnect(State.CLOSED);
        }

        @Override
        public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
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
        public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
            super.onFailure(eventSource, t, response);
            if (t != null) {
                LOGGER.warn("Unable to connect to EventSource URL", t);
            } else if (response != null) {
                LOGGER.warn("Unable to connect to EventSource URL. Status code was {}", response.code());
            } else {
                LOGGER.warn("Unable to connect to EventSource URL");
            }
            disconnect(State.FAILED);
        }

        @Override
        public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
            super.onOpen(eventSource, response);
            attempt = 0;
            transitionTo(State.CONNECTED);
        }
    }
}