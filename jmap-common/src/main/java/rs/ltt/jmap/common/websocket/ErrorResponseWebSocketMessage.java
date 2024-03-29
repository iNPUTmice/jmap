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

import lombok.Builder;
import rs.ltt.jmap.common.ErrorResponse;

public class ErrorResponseWebSocketMessage extends AbstractApiWebSocketMessage {

    private ErrorResponse response;
    private String requestId;

    @Builder
    public ErrorResponseWebSocketMessage(String requestId, final ErrorResponse response) {
        this.requestId = requestId;
        this.response = response;
    }

    public String getRequestId() {
        return this.requestId;
    }

    @Override
    public ErrorResponse getPayload() {
        return response;
    }
}
