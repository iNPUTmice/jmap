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

package rs.ltt.jmap.gson.deserializer;

import com.google.gson.*;
import java.lang.reflect.Type;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.common.entity.PushVerification;
import rs.ltt.jmap.common.entity.StateChange;

public class PushMessageDeserializer implements JsonDeserializer<PushMessage> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(PushMessage.class, new PushMessageDeserializer());
    }

    @Override
    public PushMessage deserialize(
            final JsonElement jsonElement,
            final Type type,
            final JsonDeserializationContext context)
            throws JsonParseException {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException(
                    "Expected JSON object for PushMessage. Got "
                            + jsonElement.getClass().getSimpleName());
        }
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has("@type")) {
            throw new JsonParseException("PushMessage had no @type attribute");
        }
        final String messageType = jsonObject.get("@type").getAsString();
        if ("StateChange".equals(messageType)) {
            return context.deserialize(jsonElement, StateChange.class);
        }
        if ("PushVerification".equals(messageType)) {
            return context.deserialize(jsonElement, PushVerification.class);
        }
        throw new JsonParseException(String.format("Unknown PushMessage type %s", messageType));
    }
}
