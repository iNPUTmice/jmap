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

import okhttp3.HttpUrl;
import rs.ltt.jmap.client.event.WebSocketPushService;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.client.util.WebSocketUtil;
import rs.ltt.jmap.common.entity.capability.WebSocketCapability;

public class JmapApiClientFactory {

    private final HttpAuthentication httpAuthentication;
    private final SessionStateListener sessionStateListener;

    public JmapApiClientFactory(HttpAuthentication httpAuthentication, SessionStateListener sessionStateListener) {
        this.httpAuthentication = httpAuthentication;
        this.sessionStateListener = sessionStateListener;
    }


    public JmapApiClient getJmapApiClient(final Session session, final boolean useWebSocket) {
        final WebSocketCapability webSocketCapability = session.getCapability(WebSocketCapability.class);
        if (validWebSocketCapability(webSocketCapability) && useWebSocket) {
            final HttpUrl url = WebSocketUtil.normalizeUrl(session.getBase(), webSocketCapability.getUrl());
            if (Boolean.TRUE.equals(webSocketCapability.getSupportsPush())) {
                return new WebSocketPushService(
                        url,
                        httpAuthentication,
                        sessionStateListener
                );
            } else {
                return new WebSocketJmapApiClient(
                        url,
                        httpAuthentication,
                        sessionStateListener
                );
            }
        }
        return new HttpJmapApiClient(
                session.getApiUrl(),
                httpAuthentication,
                sessionStateListener
        );
    }

    private static boolean validWebSocketCapability(final WebSocketCapability capability) {
        return capability != null && capability.getUrl() != null;
    }
}
