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

package rs.ltt.jmap.common.websocket;

import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.common.Request;

@Getter
public class RequestWebSocketMessage extends AbstractApiWebSocketMessage {

    private Request request;
    private String id;

    @Builder
    public RequestWebSocketMessage(final String id, final Request request) {
        this.id = id;
        this.request = request;
    }

    @Override
    public Request getPayload() {
        return request;
    }

    @Override
    public String getRequestId() {
        return this.id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", id).add("request", request).toString();
    }
}
