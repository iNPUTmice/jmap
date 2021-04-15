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

package rs.ltt.jmap.gson;

import com.google.common.base.Strings;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import rs.ltt.jmap.annotation.Type;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class TypeInjectionAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        final Class<? super T> clazz = typeToken.getRawType();
        final Type annotation = clazz.getAnnotation(Type.class);
        if (annotation == null) {
            return null;
        }
        final String type = Strings.isNullOrEmpty(annotation.value()) ? clazz.getSimpleName() : annotation.value();
        final TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, typeToken);
        return new TypeAdapter<T>() {
            @Override
            public void write(final JsonWriter jsonWriter, final T t) {
                final JsonElement element = delegateAdapter.toJsonTree(t);
                if (element.isJsonObject()) {
                    final JsonObject originalObject = element.getAsJsonObject();
                    final JsonObject annotatedObject = annotateWith(originalObject, type);
                    gson.toJson(annotatedObject, jsonWriter);
                } else {
                    gson.toJson(element, jsonWriter);
                }
            }

            @Override
            public T read(JsonReader jsonReader) throws IOException {
                return delegateAdapter.read(jsonReader);
            }
        };
    }

    private static JsonObject annotateWith(final JsonObject original, final String type) {
        final JsonObject annotatedObject = new JsonObject();
        annotatedObject.addProperty("@type", type);
        GsonUtils.addAll(annotatedObject, original.entrySet());
        return annotatedObject;
    }
}
