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

package rs.ltt.jmap.gson.adapter;

import static rs.ltt.jmap.gson.GsonUtils.NULL_SERIALIZING_GSON;
import static rs.ltt.jmap.gson.GsonUtils.REGULAR_GSON;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import rs.ltt.jmap.common.websocket.*;

public class ApiWebSocketMessageTypeAdapter extends TypeAdapter<AbstractApiWebSocketMessage> {

    private static final BiMap<String, Class<? extends AbstractApiWebSocketMessage>> MESSAGE_MAP =
            new ImmutableBiMap.Builder<String, Class<? extends AbstractApiWebSocketMessage>>()
                    .put("RequestError", ErrorResponseWebSocketMessage.class)
                    .put("Request", RequestWebSocketMessage.class)
                    .put("Response", ResponseWebSocketMessage.class)
                    .build();

    public static void register(final GsonBuilder builder) {
        for (final Class<? extends WebSocketMessage> clazz : MESSAGE_MAP.values()) {
            builder.registerTypeAdapter(clazz, new ApiWebSocketMessageTypeAdapter());
        }
    }

    @Override
    public void write(final JsonWriter jsonWriter, final AbstractApiWebSocketMessage message)
            throws IOException {
        jsonWriter.beginObject();

        jsonWriter.name("@type");
        jsonWriter.value(MESSAGE_MAP.inverse().get(message.getClass()));

        if (message instanceof RequestWebSocketMessage) {
            jsonWriter.name("id");
        } else {
            jsonWriter.name("requestId");
        }
        jsonWriter.value(message.getRequestId());
        final JsonElement payload = REGULAR_GSON.toJsonTree(message.getPayload());
        if (payload.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : payload.getAsJsonObject().entrySet()) {
                jsonWriter.name(entry.getKey());
                NULL_SERIALIZING_GSON.toJson(entry.getValue(), jsonWriter);
            }
        } else {
            throw new JsonIOException("Payload serialization did not yield JsonObject");
        }
        jsonWriter.endObject();
    }

    @Override
    public AbstractApiWebSocketMessage read(final JsonReader jsonReader) throws IOException {
        final WebSocketMessage message = REGULAR_GSON.fromJson(jsonReader, WebSocketMessage.class);
        if (message instanceof AbstractApiWebSocketMessage) {
            return (AbstractApiWebSocketMessage) message;
        }
        throw new IOException(
                String.format("WebSocketMessage was of type %s", message.getClass().getName()));
    }
}
