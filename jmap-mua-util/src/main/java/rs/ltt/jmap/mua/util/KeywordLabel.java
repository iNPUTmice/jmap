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

package rs.ltt.jmap.mua.util;

import com.google.common.base.CaseFormat;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import rs.ltt.jmap.common.entity.Role;

import java.io.Serializable;

public class KeywordLabel implements LabelWithCount, Serializable {

    private final String keyword;
    private final Role role;

    KeywordLabel(String keyword, Role role) {
        this.keyword = keyword;
        this.role = role;
    }

    @Override
    public String getName() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, role.name());
    }

    @Override
    public Role getRole() {
        return this.role;
    }

    @Override
    public Integer getCount() {
        return null;
    }

    public String getKeyword() {
        return keyword;
    }

    public static KeywordLabel of(final String keyword) {
        final Role role = KeywordUtil.KEYWORD_ROLE.get(keyword);
        Preconditions.checkArgument(role != null, "Keyword has no known mailbox mapping");
        return new KeywordLabel(keyword, role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeywordLabel that = (KeywordLabel) o;
        return Objects.equal(keyword, that.keyword) &&
                role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyword, role);
    }
}
