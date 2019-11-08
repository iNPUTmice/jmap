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

package rs.ltt.jmap.common.method.call.email;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.call.standard.QueryChangesMethodCall;

@JmapMethod("Email/queryChanges")
public class QueryChangesEmailMethodCall extends QueryChangesMethodCall<Email> {

    private Boolean collapseThreads;

    public QueryChangesEmailMethodCall(String accountId, String sinceQueryState, Filter<Email> filter) {
        super(accountId, sinceQueryState, filter);
    }

    public QueryChangesEmailMethodCall(String accountId, String sinceQueryState, EmailQuery query) {
        super(accountId, sinceQueryState, query);
        this.collapseThreads = query.collapseThreads;
    }

    public QueryChangesEmailMethodCall(String accountId, String sinceQueryState) {
        super(accountId, sinceQueryState);
    }
}
