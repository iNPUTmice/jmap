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
import rs.ltt.jmap.client.api.SessionStateListener;
import rs.ltt.jmap.client.api.WebSocketJmapApiClient;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.common.websocket.PushEnableWebSocketMessage;
import rs.ltt.jmap.common.websocket.StateChangeWebSocketMessage;
import rs.ltt.jmap.common.websocket.WebSocketMessage;

public class WebSocketPushService extends WebSocketJmapApiClient implements PushService {

    private OnStateChangeListener onStateChangeListener;

    private boolean enablePushNotifications = false;

    public WebSocketPushService(HttpUrl webSocketUrl, HttpAuthentication httpAuthentication, @Nullable SessionStateListener sessionStateListener) {
        super(webSocketUrl, httpAuthentication, sessionStateListener);
    }

    @Override
    public void setOnStateChangeListener(final OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    @Override
    public void connect() {
        this.enablePushNotifications = true;
        if (readyToSend()) {
            enablePushNotifications();
        }
    }

    @Override
    public void stop() {
        this.enablePushNotifications = false;
    }

    private void enablePushNotifications() {
        System.out.println("enable push notifications");
        final PushEnableWebSocketMessage message = PushEnableWebSocketMessage.builder()
                .build();
        send(message);
    }

    @Override
    protected boolean onWebSocketMessage(final WebSocketMessage message) {
        if (super.onWebSocketMessage(message)) {
            return true;
        }
        if (message instanceof StateChangeWebSocketMessage) {
            onStateChangeWebSocketMessage((StateChangeWebSocketMessage) message);
            return true;
        }
        return false;
    }

    @Override
    protected void onOpen() {
        super.onOpen();
        if (enablePushNotifications) {
            enablePushNotifications();
        }
    }

    private void onStateChangeWebSocketMessage(final StateChangeWebSocketMessage message) {
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChange(message);
        }
    }
}
