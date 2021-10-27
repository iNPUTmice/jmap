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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
import rs.ltt.jmap.common.entity.DeliveryStatus;
import rs.ltt.jmap.common.entity.EmailBodyValue;

public class StringMapSerializer implements JsonSerializer<Map<String, ?>> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(
                new TypeToken<Map<String, Boolean>>() {}.getType(), new StringMapSerializer());
        builder.registerTypeAdapter(
                new TypeToken<Map<String, EmailBodyValue>>() {}.getType(),
                new StringMapSerializer());
        builder.registerTypeAdapter(
                new TypeToken<Map<String, DeliveryStatus>>() {}.getType(),
                new StringMapSerializer());
    }

    @Override
    public JsonElement serialize(Map<String, ?> map, Type type, JsonSerializationContext context) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            jsonObject.add(entry.getKey(), context.serialize(entry.getValue()));
        }

        return jsonObject;
    }
}
