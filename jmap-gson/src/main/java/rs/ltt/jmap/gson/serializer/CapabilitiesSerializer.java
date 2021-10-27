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
import java.lang.reflect.Type;
import java.util.Map;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.util.Mapper;

public class CapabilitiesSerializer
        implements JsonSerializer<Map<Class<? extends Capability>, Capability>> {

    private static final ImmutableMap<Class<? extends Capability>, String> CAPABILITIES =
            Mapper.CAPABILITIES.inverse();

    public static void register(final GsonBuilder builder) {
        Type type = new TypeToken<Map<Class<? extends Capability>, Capability>>() {}.getType();
        builder.registerTypeAdapter(type, new CapabilitiesSerializer());
    }

    @Override
    public JsonElement serialize(
            Map<Class<? extends Capability>, Capability> map,
            Type type,
            JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        for (Map.Entry<Class<? extends Capability>, Capability> entry : map.entrySet()) {
            final Class<? extends Capability> clazz = entry.getKey();
            final String name = CAPABILITIES.get(clazz);
            jsonObject.add(
                    name != null ? name : clazz.getSimpleName(),
                    context.serialize(entry.getValue()));
        }
        return jsonObject;
    }
}
