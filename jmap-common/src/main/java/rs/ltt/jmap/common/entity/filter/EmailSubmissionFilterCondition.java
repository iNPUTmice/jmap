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

package rs.ltt.jmap.common.entity.filter;

import com.google.common.collect.ComparisonChain;
import java.time.Instant;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.common.entity.EmailSubmission;
import rs.ltt.jmap.common.entity.UndoStatus;
import rs.ltt.jmap.common.util.QueryStringUtils;

@Getter
@Builder
public class EmailSubmissionFilterCondition implements FilterCondition<EmailSubmission> {

    private String[] identityIds;

    private String[] emailIds;

    private String[] threadIds;

    private UndoStatus undoStatus;

    private Instant before;

    private Instant after;

    @Override
    public int compareTo(@Nonnull Filter<EmailSubmission> filter) {
        if (filter instanceof EmailSubmissionFilterCondition) {
            final EmailSubmissionFilterCondition other = (EmailSubmissionFilterCondition) filter;
            return ComparisonChain.start()
                    .compare(
                            identityIds,
                            other.identityIds,
                            QueryStringUtils.STRING_ARRAY_COMPARATOR)
                    .compare(emailIds, other.emailIds, QueryStringUtils.STRING_ARRAY_COMPARATOR)
                    .compare(threadIds, other.threadIds, QueryStringUtils.STRING_ARRAY_COMPARATOR)
                    .compare(
                            QueryStringUtils.nullToEmpty(undoStatus),
                            QueryStringUtils.nullToEmpty(other.undoStatus))
                    .compare(before, other.before)
                    .compare(after, other.after, QueryStringUtils.INSTANT_COMPARATOR)
                    .result();
        } else {
            return 1;
        }
    }

    @Override
    public String toQueryString() {
        return QueryStringUtils.toQueryString(
                L3_DIVIDER,
                L4_DIVIDER,
                identityIds,
                emailIds,
                threadIds,
                undoStatus,
                before,
                after);
    }
}
