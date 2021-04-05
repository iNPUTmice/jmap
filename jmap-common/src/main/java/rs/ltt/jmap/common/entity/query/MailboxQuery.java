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

import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.util.QueryStringUtils;

public class MailboxQuery extends Query<Mailbox> {

    public final Boolean sortAsTree;

    public final Boolean filterAsTree;

    private MailboxQuery(Filter<Mailbox> filter, Comparator[] sort, Boolean sortAsTree, Boolean filterAsTree) {
        super(filter, sort);
        this.sortAsTree = sortAsTree;
        this.filterAsTree = filterAsTree;
    }

    @Override
    public String toQueryString() {
        return QueryStringUtils.toQueryString(L0_DIVIDER, L1_DIVIDER, filter, sort, sortAsTree, filterAsTree);
    }

    public static MailboxQuery unfiltered() {
        return new MailboxQuery(null, null, null, null);
    }

    public static MailboxQuery unfiltered(Boolean sortAsTree, Boolean filterAsTree) {
        return new MailboxQuery(null, null, sortAsTree, filterAsTree);
    }

    public static MailboxQuery of(Filter<Mailbox> filter) {
        return new MailboxQuery(filter, null, null, null);
    }

    public static MailboxQuery of(final Filter<Mailbox> filter, final Comparator[] sort) {
        return new MailboxQuery(filter, sort, null, null);
    }

    public static MailboxQuery of(Filter<Mailbox> filter, Comparator[] sort, Boolean sortAsTree, Boolean filterAsTree) {
        return new MailboxQuery(filter, sort, sortAsTree, filterAsTree);
    }
}
