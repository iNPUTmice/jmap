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

import okhttp3.HttpUrl;
import org.jetbrains.annotations.Nullable;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.api.SessionStateListener;
import rs.ltt.jmap.client.api.WebSocketJmapApiClient;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.util.State;
import rs.ltt.jmap.common.websocket.PushDisableWebSocketMessage;
import rs.ltt.jmap.common.websocket.PushEnableWebSocketMessage;
import rs.ltt.jmap.common.websocket.StateChangeWebSocketMessage;
import rs.ltt.jmap.common.websocket.WebSocketMessage;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class WebSocketPushService extends WebSocketJmapApiClient implements PushService, OnStateChangeListenerManager.Callback {

    private final OnStateChangeListenerManager onStateChangeListenerManager = new OnStateChangeListenerManager(this);
    private ReconnectionStrategy reconnectionStrategy = ReconnectionStrategy.truncatedBinaryExponentialBackoffStrategy(60, 4);

    public WebSocketPushService(HttpUrl webSocketUrl, HttpAuthentication httpAuthentication, @Nullable SessionStateListener sessionStateListener) {
        super(webSocketUrl, httpAuthentication, sessionStateListener);
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
    public void disable() {
        if (state == State.CONNECTED) {
            disablePushNotifications();
        }
    }

    private void disablePushNotifications() {
        LOGGER.info("Disable push notifications");
        final PushDisableWebSocketMessage message = PushDisableWebSocketMessage.builder()
                .build();
        send(message);
    }

    @Override
    public void enable() {
        if (readyToSend()) {
            enablePushNotifications();
        }
    }

    private void enablePushNotifications() {
        LOGGER.info("Enable push notifications");
        final PushEnableWebSocketMessage message = PushEnableWebSocketMessage.builder()
                .build();
        send(message);
    }

    @Override
    protected void transitionTo(final State state) {
        super.transitionTo(state);
        if (state.needsReconnect() && this.onStateChangeListenerManager.isPushNotificationsEnabled()) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        final int attempt = this.attempt;
        final Duration reconnectIn = reconnectionStrategy.getNextReconnectionAttempt(attempt);
        LOGGER.info("schedule reconnect in {} for {} time", reconnectIn, attempt + 1);
        this.reconnectionFuture = Services.SCHEDULED_EXECUTOR_SERVICE.schedule(
                this::connectWebSocket,
                reconnectIn.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected boolean onWebSocketMessage(final WebSocketMessage message) {
        if (super.onWebSocketMessage(message)) {
            return true;
        }
        if (message instanceof StateChangeWebSocketMessage) {
            this.onStateChangeListenerManager.onStateChange((StateChangeWebSocketMessage) message);
            return true;
        }
        return false;
    }

    @Override
    protected void onOpen() {
        super.onOpen();
        if (this.onStateChangeListenerManager.isPushNotificationsEnabled()) {
            enablePushNotifications();
        }
    }

    @Override
    public void close() {
        this.onStateChangeListenerManager.removeAllListeners();
        super.close();
    }

}
