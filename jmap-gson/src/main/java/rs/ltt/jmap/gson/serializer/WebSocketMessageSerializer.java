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

package rs.ltt.jmap.gson.serializer;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.*;
import rs.ltt.jmap.common.websocket.*;
import rs.ltt.jmap.gson.GsonUtils;

import java.lang.reflect.Type;

public class WebSocketMessageSerializer implements JsonSerializer<WebSocketMessage> {

    private static final BiMap<String, Class<? extends WebSocketMessage>> MESSAGE_MAP = new ImmutableBiMap.Builder<String, Class<? extends WebSocketMessage>>()
            .put("RequestError", ErrorResponseWebSocketMessage.class)
            .put("WebSocketPushEnable", PushEnableWebSocketMessage.class)
            .put("WebSocketPushDisable", PushDisableWebSocketMessage.class)
            .put("Request", RequestWebSocketMessage.class)
            .put("Response", ResponseWebSocketMessage.class)
            .build();

    public static void register(final GsonBuilder builder) {
        for (final Class<? extends WebSocketMessage> clazz : MESSAGE_MAP.values()) {
            builder.registerTypeAdapter(clazz, new WebSocketMessageSerializer());
        }
    }

    @Override
    public JsonElement serialize(final WebSocketMessage message, final Type type, final JsonSerializationContext context) {
        if (message instanceof AbstractApiWebSocketMessage) {
            return serialize((AbstractApiWebSocketMessage) message, context);
        }
        if (message instanceof PushDisableWebSocketMessage) {
            return serialize((PushDisableWebSocketMessage) message, context);
        }
        if (message instanceof PushEnableWebSocketMessage) {
            return serialize((PushEnableWebSocketMessage) message, context);
        }
        throw new JsonIOException(String.format("%s is not a registered WebSocketMessage", message.getClass().getName()));
    }

    private JsonElement serialize(final PushDisableWebSocketMessage message, final JsonSerializationContext context) {
        return null;
    }

    private JsonElement serialize(final PushEnableWebSocketMessage message, final JsonSerializationContext context) {
        return null;
    }

    private JsonElement serialize(final AbstractApiWebSocketMessage message, final JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("@type", MESSAGE_MAP.inverse().get(message.getClass()));
        jsonObject.addProperty("requestId", message.getRequestId());
        final JsonElement payload = context.serialize(message.getPayload());
        if (payload.isJsonObject()) {
            GsonUtils.addAll(jsonObject, payload.getAsJsonObject());
        } else {
            throw new JsonIOException("Payload serialization did not yield JsonObject");
        }
        return jsonObject;
    }
}
