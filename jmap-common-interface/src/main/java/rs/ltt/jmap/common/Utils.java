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

package rs.ltt.jmap.common;

public final class Utils {

    private Utils() {

    }

    public static String getFilenameFor(final Class<?> clazz) {
        return "META-INF/"+plural(clazz.getName());
    }

    private static String plural(String input) {
        final char c = input.charAt(input.length() - 1);
        if (c == 'y') {
            return input.substring(0, input.length() - 1) + "ies";
        } else {
            return input + "s";
        }
    }
}
