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

package rs.ltt.jmap.common.entity.query;

import com.google.common.base.MoreObjects;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.util.QueryStringUtils;

public class EmailQuery extends Query<Email> {

    public final Boolean collapseThreads;

    private EmailQuery(final Filter<Email> filter, final Comparator[] sort, final Boolean collapseThreads) {
        super(filter, sort);
        this.collapseThreads = collapseThreads;
    }

    @Override
    public String toQueryString() {
        return QueryStringUtils.toQueryString(L0_DIVIDER, L1_DIVIDER, filter, sort, collapseThreads);
    }

    public static EmailQuery unfiltered() {
        return new EmailQuery(null, null, null);
    }

    public static EmailQuery unfiltered(boolean collapseThreads) {
        return new EmailQuery(null, null, collapseThreads);
    }

    public static EmailQuery of(Filter<Email> filter) {
        return new EmailQuery(filter, null, null);
    }

    public static EmailQuery of(Filter<Email> filter, Comparator[] sort) {
        return new EmailQuery(filter, sort, null);
    }

    public static EmailQuery of(Filter<Email> filter, boolean collapseThreads) {
        return new EmailQuery(filter, null, collapseThreads);
    }

    public static EmailQuery of(Filter<Email> filter, Comparator[] sort, boolean collapseThreads) {
        return new EmailQuery(filter, sort, collapseThreads);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("collapseThreads", collapseThreads)
                .add("filter", filter)
                .add("sort", sort)
                .omitNullValues()
                .toString();
    }
}
