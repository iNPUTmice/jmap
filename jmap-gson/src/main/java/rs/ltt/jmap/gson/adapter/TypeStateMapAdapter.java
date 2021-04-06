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

import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.util.Mapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class TypeStateMapAdapter extends TypeAdapter<Map<Class<? extends AbstractIdentifiableEntity>,String>> {

    public static void register(final GsonBuilder builder) {
        final Type type = new TypeToken<Map<Class<? extends AbstractIdentifiableEntity>, String>>() {
        }.getType();
        builder.registerTypeAdapter(type, new TypeStateMapAdapter());
    }

    @Override
    public void write(final JsonWriter jsonWriter, final Map<Class<? extends AbstractIdentifiableEntity>, String> typeStateMap) throws IOException {
        jsonWriter.beginObject();
        for(final Map.Entry<Class<? extends AbstractIdentifiableEntity>,String> entry : typeStateMap.entrySet()) {
            final Class<? extends AbstractIdentifiableEntity> entityClazz = entry.getKey();
            final String entityType = Mapper.ENTITIES.inverse().get(entityClazz);
            if (entityType == null) {
                throw new JsonIOException(String.format("%s is not a registered @JmapEntity", entityClazz.getSimpleName()));
            }
            final String state = entry.getValue();
            jsonWriter.name(entityType);
            jsonWriter.value(state);
        }
        jsonWriter.endObject();
    }

    @Override
    public Map<Class<? extends AbstractIdentifiableEntity>, String> read(final JsonReader jsonReader) throws IOException {
        final ImmutableMap.Builder<Class<? extends AbstractIdentifiableEntity>, String> mapBuilder = new ImmutableMap.Builder<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            final String entityType = jsonReader.nextName();
            final String state = jsonReader.nextString();
            final Class<? extends AbstractIdentifiableEntity> entityClazz = Mapper.ENTITIES.get(entityType);
            if (entityClazz == null) {
                //TODO do we want to log this?
                continue;
            }
            mapBuilder.put(entityClazz, state);
        }
        jsonReader.endObject();
        return mapBuilder.build();
    }
}
