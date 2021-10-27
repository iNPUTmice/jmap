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
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Response;

public class GenericResponseDeserializer implements JsonDeserializer<GenericResponse> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(GenericResponse.class, new GenericResponseDeserializer());
    }

    @Override
    public GenericResponse deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("type") && !jsonObject.has("methodResponses")) {
                return context.deserialize(jsonObject, ErrorResponse.class);
            }
            if (jsonObject.has("methodResponses") && !jsonObject.has("type")) {
                return context.deserialize(jsonObject, Response.class);
            }
            throw new JsonParseException(
                    "Unable to identify response as neither error nor response");
        } else {
            throw new JsonParseException("unexpected json type when parsing response");
        }
    }
}
