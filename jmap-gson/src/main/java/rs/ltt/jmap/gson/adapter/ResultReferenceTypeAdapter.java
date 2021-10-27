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

package rs.ltt.jmap.gson.adapter;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.util.Mapper;

public class ResultReferenceTypeAdapter extends TypeAdapter<Request.Invocation.ResultReference> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(
                Request.Invocation.ResultReference.class, new ResultReferenceTypeAdapter());
    }

    @Override
    public void write(
            JsonWriter jsonWriter, final Request.Invocation.ResultReference resultReference)
            throws IOException {
        if (resultReference == null) {
            jsonWriter.nullValue();
            return;
        }
        jsonWriter.beginObject();
        jsonWriter.name("resultOf").value(resultReference.getId());
        jsonWriter
                .name("name")
                .value(Mapper.METHOD_CALLS.inverse().get(resultReference.getClazz()));
        jsonWriter.name("path").value(resultReference.getPath());
        jsonWriter.endObject();
    }

    @Override
    public Request.Invocation.ResultReference read(JsonReader jsonReader) throws IOException {
        String name = null;
        String path = null;
        String id = null;
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "resultOf":
                    id = jsonReader.nextString();
                    break;
                case "path":
                    path = jsonReader.nextString();
                    break;
                case "name":
                    name = jsonReader.nextString();
                    break;
            }
        }
        jsonReader.endObject();
        try {

            Constructor<Request.Invocation.ResultReference> constructor =
                    Request.Invocation.ResultReference.class.getDeclaredConstructor(
                            String.class, Class.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(id, Mapper.METHOD_CALLS.get(name), path);
        } catch (NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
