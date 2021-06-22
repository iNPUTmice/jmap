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
 */

package rs.ltt.jmap.mua.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithKeywords;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Role;

import java.util.Collection;


public class KeywordUtil {

    public static final BiMap<String, Role> KEYWORD_ROLE = new ImmutableBiMap.Builder<String, Role>()
            .put(Keyword.FLAGGED,Role.FLAGGED)
            .put(Keyword.DRAFT, Role.DRAFTS)
            .build();

    public static boolean anyHas(Collection<?extends IdentifiableEmailWithKeywords> emails, String keyword) {
        for(IdentifiableEmailWithKeywords email : emails) {
            if (email.getKeywords().containsKey(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static boolean everyHas(Collection<?extends IdentifiableEmailWithKeywords> emails, String keyword) {
        for(IdentifiableEmailWithKeywords email : emails) {
            if (!email.getKeywords().containsKey(keyword)) {
                return false;
            }
        }
        return true;
    }

    public static boolean seen(final IdentifiableEmailWithKeywords email) {
        return email.getKeywords().getOrDefault(Keyword.SEEN, false);
    }

    public static boolean draft(final IdentifiableEmailWithKeywords email) {
        return email.getKeywords().getOrDefault(Keyword.DRAFT, false);
    }
}
