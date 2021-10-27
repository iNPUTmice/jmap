/*
 * Copyright 2020 Daniel Gultsch
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

package rs.ltt.jmap.client;

import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.call.standard.QueryMethodCall;

public class QueryDummyMethodCall extends QueryMethodCall<Dummy> {

    public QueryDummyMethodCall(
            String accountId,
            Filter<Dummy> filter,
            Comparator[] sort,
            Long position,
            String anchor,
            Long anchorOffset,
            Long limit,
            Boolean calculateTotal) {
        super(accountId, filter, sort, position, anchor, anchorOffset, limit, calculateTotal);
    }
}
