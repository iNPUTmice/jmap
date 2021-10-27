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

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.util.QueryStringUtils;

@Getter
@Builder
public class MailboxFilterCondition implements FilterCondition<Mailbox> {

    private String parentId;

    private String name;

    private Role role;

    private Boolean hasAnyRole;

    private Boolean isSubscribed;

    @Override
    public int compareTo(@Nonnull Filter<Mailbox> filter) {
        if (filter instanceof MailboxFilterCondition) {
            final MailboxFilterCondition other = (MailboxFilterCondition) filter;
            return ComparisonChain.start()
                    .compare(Strings.nullToEmpty(parentId), Strings.nullToEmpty(other.parentId))
                    .compare(Strings.nullToEmpty(name), Strings.nullToEmpty(other.name))
                    .compare(
                            QueryStringUtils.nullToEmpty(role),
                            QueryStringUtils.nullToEmpty(other.role))
                    .compare(hasAnyRole, other.hasAnyRole, QueryStringUtils.BOOLEAN_COMPARATOR)
                    .compare(isSubscribed, other.isSubscribed, QueryStringUtils.BOOLEAN_COMPARATOR)
                    .result();
        } else {
            return 1;
        }
    }

    @Override
    public String toQueryString() {
        return QueryStringUtils.toQueryString(
                L3_DIVIDER, L4_DIVIDER, parentId, name, role, hasAnyRole, isSubscribed);
    }
}
