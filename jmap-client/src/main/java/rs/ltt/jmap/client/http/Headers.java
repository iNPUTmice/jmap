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

package rs.ltt.jmap.client.http;

public final class Headers {

    public static final String SEC_WEB_SOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    private Headers() {
        throw new IllegalStateException("Do not instantiate me");
    }
}
