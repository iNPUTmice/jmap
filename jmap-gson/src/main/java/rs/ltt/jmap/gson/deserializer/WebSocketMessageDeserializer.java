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

package rs.ltt.jmap.gson.deserializer;

import com.google.gson.*;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.websocket.*;

import java.lang.reflect.Type;

public class WebSocketMessageDeserializer implements JsonDeserializer<WebSocketMessage> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(WebSocketMessage.class, new WebSocketMessageDeserializer());
    }

    @Override
    public WebSocketMessage deserialize(final JsonElement jsonElement,
                                        final Type type,
                                        final JsonDeserializationContext context) throws JsonParseException {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Expected JSON object for WebSocketMessage. Got " + jsonElement.getClass().getSimpleName());
        }
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has("@type")) {
            throw new JsonParseException("WebSocketMessage had no @type attribute");
        }
        final String messageType = jsonObject.get("@type").getAsString();
        final String requestId = getAsString(jsonObject, "requestId");
        final String id = getAsString(jsonObject, "id");
        if ("Response".equals(messageType)) {
            final Response response = context.deserialize(jsonElement, Response.class);
            return ResponseWebSocketMessage.builder()
                    .requestId(requestId)
                    .response(response)
                    .build();
        }
        if ("ErrorResponse".equals(messageType)) {
            final ErrorResponse errorResponse = context.deserialize(jsonElement, ErrorResponse.class);
            return ErrorResponseWebSocketMessage.builder()
                    .requestId(requestId)
                    .response(errorResponse)
                    .build();
        }
        if ("StateChange".equals(messageType)) {
            return context.deserialize(jsonElement, StateChangeWebSocketMessage.class);
        }
        if ("Request".equals(messageType)) {
            final Request request = context.deserialize(jsonElement, Request.class);
            return RequestWebSocketMessage.builder()
                    .id(id)
                    .request(request)
                    .build();
        }
        if ("WebSocketPushEnable".equals(messageType)) {
            return context.deserialize(jsonElement, PushEnableWebSocketMessage.class);
        }
        if ("WebSocketPushDisable".equals(messageType)) {
            return context.deserialize(jsonElement, PushDisableWebSocketMessage.class);
        }
        throw new JsonParseException(String.format("Unknown WebSocketMessage type %s", messageType));
    }

    private static String getAsString(final JsonObject jsonObject, final String name) {
        if (jsonObject.has(name)) {
            final JsonElement element = jsonObject.get(name);
            return element.isJsonNull() ? null : jsonObject.get(name).getAsString();
        } else {
            return null;
        }
    }
}
