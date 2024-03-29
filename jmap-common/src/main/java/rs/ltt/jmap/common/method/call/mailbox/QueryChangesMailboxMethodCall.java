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

package rs.ltt.jmap.common.method.call.mailbox;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.query.MailboxQuery;
import rs.ltt.jmap.common.method.call.standard.QueryChangesMethodCall;

@JmapMethod("Mailbox/queryChanges")
public class QueryChangesMailboxMethodCall extends QueryChangesMethodCall<Mailbox> {

    @Builder
    public QueryChangesMailboxMethodCall(
            String accountId,
            Filter<Mailbox> filter,
            Comparator[] sort,
            String sinceQueryState,
            Long maxChanges,
            String upToId,
            Boolean calculateTotal) {
        super(accountId, filter, sort, sinceQueryState, maxChanges, upToId, calculateTotal);
    }

    public static class QueryChangesMailboxMethodCallBuilder {
        public QueryChangesMailboxMethodCallBuilder query(MailboxQuery query) {
            filter(query.filter);
            sort(query.sort);
            return this;
        }
    }
}
