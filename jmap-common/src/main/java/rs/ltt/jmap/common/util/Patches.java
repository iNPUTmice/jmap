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

package rs.ltt.jmap.common.util;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class Patches {

    private Patches() {}

    public static Map<String, Object> set(String path, Object value) {
        return new Builder().set(path, value).build();
    }

    public static Map<String, Object> remove(String path) {
        return new Builder().remove(path).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Null {
        private Null() {}
    }

    public static class Builder {

        private Builder() {}

        private final ImmutableMap.Builder<String, Object> mapBuilder =
                new ImmutableMap.Builder<>();

        public Builder set(String path, Object value) {
            mapBuilder.put(path, value);
            return this;
        }

        public Builder remove(String path) {
            mapBuilder.put(path, new Null());
            return this;
        }

        public Map<String, Object> build() {
            return mapBuilder.build();
        }
    }
}
