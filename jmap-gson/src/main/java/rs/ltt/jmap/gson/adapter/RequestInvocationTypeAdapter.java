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

import static rs.ltt.jmap.common.util.Mapper.METHOD_CALLS;
import static rs.ltt.jmap.gson.GsonUtils.NULL_SERIALIZING_GSON;
import static rs.ltt.jmap.gson.GsonUtils.REGULAR_GSON;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.MethodCall;

public class RequestInvocationTypeAdapter extends TypeAdapter<Request.Invocation> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(Request.Invocation.class, new RequestInvocationTypeAdapter());
    }

    @Override
    public void write(JsonWriter jsonWriter, Request.Invocation invocation) throws IOException {
        final MethodCall methodCall = invocation.getMethodCall();
        final Class<? extends MethodCall> clazz = methodCall.getClass();
        final String name = METHOD_CALLS.inverse().get(clazz);
        if (name == null) {
            throw new JsonIOException(
                    String.format("%s is not a registered @JmapMethod", clazz.getName()));
        }
        jsonWriter.beginArray();
        jsonWriter.value(name);
        NULL_SERIALIZING_GSON.toJson(REGULAR_GSON.toJsonTree(methodCall), jsonWriter);
        jsonWriter.value(invocation.getId());
        jsonWriter.endArray();
    }

    @Override
    public Request.Invocation read(final JsonReader jsonReader) throws IOException {
        jsonReader.beginArray();
        final String name = jsonReader.nextString();
        final Class<? extends MethodCall> clazz = METHOD_CALLS.get(name);
        final MethodCall methodCall = REGULAR_GSON.fromJson(jsonReader, clazz);
        final String id = jsonReader.nextString();
        jsonReader.endArray();
        return new Request.Invocation(methodCall, id);
    }
}
