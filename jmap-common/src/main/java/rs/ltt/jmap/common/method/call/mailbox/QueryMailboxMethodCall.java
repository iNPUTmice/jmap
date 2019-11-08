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

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.query.MailboxQuery;
import rs.ltt.jmap.common.method.call.standard.QueryMethodCall;

@JmapMethod("Mailbox/query")
public class QueryMailboxMethodCall extends QueryMethodCall<Mailbox> {

    private Boolean sortAsTree;

    private Boolean filterAsTree;

    public QueryMailboxMethodCall(String accountId, Filter<Mailbox> filter) {
        super(accountId, filter);
    }

    public QueryMailboxMethodCall(String accountId, MailboxQuery query) {
        super(accountId, query);
        this.sortAsTree = query.sortAsTree;
        this.filterAsTree = query.filterAsTree;
    }

    public QueryMailboxMethodCall(String accountId, MailboxQuery query, String afterId) {
        super(accountId, query, afterId);
        this.sortAsTree = query.sortAsTree;
        this.filterAsTree = query.filterAsTree;
    }

    public QueryMailboxMethodCall(String accountId, MailboxQuery query, Long limit) {
        super(accountId, query, limit);
        this.sortAsTree = query.sortAsTree;
        this.filterAsTree = query.filterAsTree;
    }

    public QueryMailboxMethodCall(String accountId, MailboxQuery query, String afterId, Long limit) {
        super(accountId, query, afterId, limit);
        this.sortAsTree = query.sortAsTree;
        this.filterAsTree = query.filterAsTree;
    }

}
