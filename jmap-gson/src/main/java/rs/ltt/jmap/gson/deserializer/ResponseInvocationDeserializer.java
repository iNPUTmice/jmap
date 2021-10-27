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

import com.google.gson.*;
import java.lang.reflect.Type;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.util.Mapper;

public class ResponseInvocationDeserializer implements JsonDeserializer<Response.Invocation> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(
                Response.Invocation.class, new ResponseInvocationDeserializer());
    }

    @Override
    public Response.Invocation deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        if (!jsonElement.isJsonArray()) {
            throw new JsonParseException(
                    "Expected JSON array for invocation. Got "
                            + jsonElement.getClass().getSimpleName());
        }
        final JsonArray jsonArray = jsonElement.getAsJsonArray();
        if (jsonArray.size() != 3) {
            throw new JsonParseException(
                    "Invocation array has " + jsonArray.size() + " values. Expected 3");
        }
        final String name;
        if (jsonArray.get(0).isJsonPrimitive()) {
            name = jsonArray.get(0).getAsString();
        } else {
            throw new JsonParseException("Name (index 0 of JsonArray) must be a primitive string");
        }
        final JsonElement parameter = jsonArray.get(1);
        final String id = jsonArray.get(2).getAsString();

        if (!parameter.isJsonObject()) {
            throw new JsonParseException("Parameter (index 1 of JsonArray) must be of type object");
        }

        final Class<? extends MethodResponse> clazz;
        if ("error".equals(name)) {
            final JsonObject jsonObject = parameter.getAsJsonObject();
            final String errorType = jsonObject.get("type").getAsString();
            Class<? extends MethodErrorResponse> customErrorClazz =
                    Mapper.METHOD_ERROR_RESPONSES.get(errorType);
            clazz = customErrorClazz != null ? customErrorClazz : MethodErrorResponse.class;
        } else {
            clazz = Mapper.METHOD_RESPONSES.get(name);
        }
        if (clazz == null) {
            throw new UnknownMethodNameException(name);
        }
        final MethodResponse methodResponse = context.deserialize(parameter, clazz);
        return new Response.Invocation(methodResponse, id);
    }

    public static class UnknownMethodNameException extends JsonParseException {
        private final String name;

        public UnknownMethodNameException(final String name) {
            super(String.format("Unknown method name '%s'", name));
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
