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

package rs.ltt.jmap.gson.serializer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.util.Mapper;

import java.lang.reflect.Type;
import java.util.Map;

public class AccountCapabilitiesSerializer implements JsonSerializer<Map<Class<? extends AccountCapability>, AccountCapability>> {

    private static final ImmutableMap<Class<? extends AccountCapability>,String> ACCOUNT_CAPABILITIES = Mapper.ACCOUNT_CAPABILITIES.inverse();

    public static void register(final GsonBuilder builder) {
        Type type = new TypeToken<Map<Class<? extends AccountCapability>, AccountCapability>>() {
        }.getType();
        builder.registerTypeAdapter(type, new AccountCapabilitiesSerializer());
    }

    @Override
    public JsonElement serialize(Map<Class<? extends AccountCapability>, AccountCapability> map, Type type, JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        for (Map.Entry<Class<? extends AccountCapability>, AccountCapability> entry : map.entrySet()) {
            final Class<? extends AccountCapability> clazz = entry.getKey();
            final String name = ACCOUNT_CAPABILITIES.get(clazz);
            jsonObject.add(name != null ? name : clazz.getSimpleName(), context.serialize(entry.getValue()));
        }
        return jsonObject;
    }
}
