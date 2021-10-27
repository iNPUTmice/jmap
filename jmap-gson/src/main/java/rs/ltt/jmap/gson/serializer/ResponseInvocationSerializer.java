/*
 * Copyright 2020 Daniel Gultsch
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
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.util.Mapper;

public class ResponseInvocationSerializer implements JsonSerializer<Response.Invocation> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(Response.Invocation.class, new ResponseInvocationSerializer());
    }

    @Override
    public JsonElement serialize(
            final Response.Invocation invocation,
            final Type type,
            final JsonSerializationContext context) {
        final String id = invocation.getId();
        final MethodResponse methodResponse = invocation.getMethodResponse();
        final JsonArray jsonArray = new JsonArray();
        if (methodResponse instanceof MethodErrorResponse) {
            jsonArray.add("error");
            final String errorType =
                    Mapper.METHOD_ERROR_RESPONSES.inverse().get(methodResponse.getClass());
            if (errorType == null) {
                throw new JsonIOException(
                        String.format(
                                "Unable to serialize %s. Did you annotate the Method with"
                                        + " @JmapError?",
                                methodResponse.getClass().getSimpleName()));
            }
            final JsonObject jsonObject = (JsonObject) context.serialize(methodResponse);
            jsonObject.addProperty("type", errorType);
            jsonArray.add(jsonObject);
        } else {
            final String name = Mapper.METHOD_RESPONSES.inverse().get(methodResponse.getClass());
            if (name == null) {
                throw new JsonIOException(
                        String.format(
                                "Unable to serialize %s. Did you annotate the method with"
                                        + " @JmapMethod?",
                                methodResponse.getClass().getSimpleName()));
            }
            jsonArray.add(name);
            jsonArray.add(context.serialize(methodResponse));
        }
        jsonArray.add(id);
        return jsonArray;
    }
}
