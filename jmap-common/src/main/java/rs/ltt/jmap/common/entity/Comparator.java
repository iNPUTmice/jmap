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

package rs.ltt.jmap.common.entity;

import com.google.common.base.MoreObjects;
import rs.ltt.jmap.common.entity.filter.QueryString;
import rs.ltt.jmap.common.util.QueryStringUtils;

public class Comparator implements QueryString {

    private String property;

    private Boolean isAscending;

    private String collation;

    public Comparator(String property, Boolean isAscending) {
        this(property, isAscending, null);
    }

    public Comparator(String property, Boolean isAscending, String collation) {
        this.property = property;
        this.isAscending = isAscending;
        this.collation = collation;
    }

    @Override
    public String toQueryString() {
        return QueryStringUtils.toQueryString(
                L2_DIVIDER, L3_DIVIDER, property, isAscending, collation);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("property", property)
                .add("isAscending", isAscending)
                .add("collation", collation)
                .toString();
    }
}
