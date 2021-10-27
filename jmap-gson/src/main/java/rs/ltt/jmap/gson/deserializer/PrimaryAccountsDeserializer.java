/*
 * Copyright 2019 Daniel Gultsch
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

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.util.Mapper;

public class PrimaryAccountsDeserializer
        implements JsonDeserializer<Map<Class<? extends AccountCapability>, String>> {

    public static void register(final GsonBuilder builder) {
        Type type = new TypeToken<Map<Class<? extends AccountCapability>, String>>() {}.getType();
        builder.registerTypeAdapter(type, new PrimaryAccountsDeserializer());
    }

    public Map<Class<? extends AccountCapability>, String> deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        ImmutableMap.Builder<Class<? extends AccountCapability>, String> builder =
                new ImmutableMap.Builder<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            final String namespace = entry.getKey();
            final Class<? extends AccountCapability> clazz =
                    Mapper.ACCOUNT_CAPABILITIES.get(namespace);
            if (clazz == null) {
                continue;
            }
            final String id = context.deserialize(entry.getValue(), String.class);
            builder.put(clazz, id);
        }
        return builder.build();
    }
}
