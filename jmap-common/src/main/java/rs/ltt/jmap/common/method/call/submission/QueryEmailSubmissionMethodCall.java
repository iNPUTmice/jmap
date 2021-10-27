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

package rs.ltt.jmap.common.method.call.submission;

import lombok.Builder;
import lombok.NonNull;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.EmailSubmission;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.query.EmailSubmissionQuery;
import rs.ltt.jmap.common.method.call.standard.QueryMethodCall;

@JmapMethod("EmailSubmission/query")
public class QueryEmailSubmissionMethodCall extends QueryMethodCall<EmailSubmission> {

    @Builder
    public QueryEmailSubmissionMethodCall(
            @NonNull String accountId,
            Filter<EmailSubmission> filter,
            Comparator[] sort,
            Long position,
            String anchor,
            Long anchorOffset,
            Long limit,
            Boolean calculateTotal) {
        super(accountId, filter, sort, position, anchor, anchorOffset, limit, calculateTotal);
    }

    public static class QueryEmailSubmissionMethodCallBuilder {
        public QueryEmailSubmissionMethodCallBuilder query(EmailSubmissionQuery query) {
            filter(query.filter);
            sort(query.sort);
            return this;
        }
    }
}
