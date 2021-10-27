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
import java.lang.reflect.Type;
import java.util.List;

public class ListSerializer implements JsonSerializer<List<?>> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(List.class, new ListSerializer());
    }

    @Override
    public JsonElement serialize(List<?> list, Type type, JsonSerializationContext context) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        final JsonArray array = new JsonArray();
        for (final Object child : list) {
            final JsonElement element = context.serialize(child);
            array.add(element);
        }

        return array;
    }
}
