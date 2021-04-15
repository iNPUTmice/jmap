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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

public final class GsonUtils {

    private GsonUtils() {

    }

    public static void addAll(final JsonObject to, JsonObject object) {
        addAll(to, object.entrySet());
    }

    public static void addAll(final JsonObject to, Set<Map.Entry<String, JsonElement>> entries) {
        for (final Map.Entry<String, JsonElement> entry : entries) {
            to.add(entry.getKey(), entry.getValue());
        }
    }
}
