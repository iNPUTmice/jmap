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

package rs.ltt.jmap.gson.deserializer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.filter.FilterOperator;
import rs.ltt.jmap.common.util.Mapper;

public class FilterDeserializer
        implements JsonDeserializer<Filter<? extends AbstractIdentifiableEntity>> {

    public static void register(final GsonBuilder builder) {
        for (final Type type : Mapper.TYPE_TO_ENTITY_CLASS.keySet()) {
            builder.registerTypeAdapter(type, new FilterDeserializer());
        }
    }

    @Override
    public Filter<? extends AbstractIdentifiableEntity> deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final boolean isOperator = jsonObject.has("operator") && jsonObject.has("conditions");
        final Class<? extends AbstractIdentifiableEntity> entityClass =
                Mapper.TYPE_TO_ENTITY_CLASS.get(type);
        if (isOperator) {
            return context.deserialize(
                    jsonElement,
                    TypeToken.getParameterized(FilterOperator.class, entityClass).getType());
        } else {
            return context.deserialize(
                    jsonElement, Mapper.ENTITY_TO_FILTER_CONDITION.get(entityClass));
        }
    }
}
