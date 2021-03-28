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

package rs.ltt.jmap.mua.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AccountUtil {

    private AccountUtil() {

    }

    public static String printableName(final String username) {
        final int atPosition = username.indexOf("@");
        if (atPosition <= 0) {
            return username;
        }
        final List<String> words = Splitter.onPattern("[\\.\\-_]")
                .splitToStream(username.substring(0, atPosition))
                .filter(word -> word.length() > 0)
                .map(AccountUtil::capitalizeFirst)
                .collect(Collectors.toList());
        if (words.size() > 0) {
            return Joiner.on(' ').join(words);
        } else {
            return username;
        }
    }

    private static String capitalizeFirst(final String word) {
        return word.substring(0, 1).toUpperCase(Locale.ENGLISH) + word.substring(1).toLowerCase(Locale.ENGLISH);
    }
}
