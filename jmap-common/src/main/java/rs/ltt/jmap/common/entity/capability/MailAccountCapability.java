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

package rs.ltt.jmap.common.entity.capability;

import com.google.common.base.MoreObjects;
import rs.ltt.jmap.Namespace;
import rs.ltt.jmap.annotation.JmapAccountCapability;
import rs.ltt.jmap.common.entity.AccountCapability;

@JmapAccountCapability(namespace = Namespace.MAIL)
public class MailAccountCapability implements AccountCapability {
    private Long maxMailboxesPerEmail;
    private Long maxMailboxDepth;
    private long maxSizeMailboxName;
    private long maxSizeAttachmentsPerEmail;
    private String[] emailQuerySortOptions;
    private boolean mayCreateTopLevelMailbox;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxMailboxesPerEmail", maxMailboxesPerEmail)
                .add("maxMailboxDepth", maxMailboxDepth)
                .add("maxSizeMailboxName", maxSizeMailboxName)
                .add("maxSizeAttachmentsPerEmail", maxSizeAttachmentsPerEmail)
                .add("emailQuerySortOptions", emailQuerySortOptions)
                .add("mayCreateTopLevelMailbox", mayCreateTopLevelMailbox)
                .toString();
    }

    public Long getMaxMailboxesPerEmail() {
        return maxMailboxesPerEmail;
    }

    public Long getMaxMailboxDepth() {
        return maxMailboxDepth;
    }

    public long getMaxSizeMailboxName() {
        return maxSizeMailboxName;
    }

    public long getMaxSizeAttachmentsPerEmail() {
        return maxSizeAttachmentsPerEmail;
    }

    public String[] getEmailQuerySortOptions() {
        return emailQuerySortOptions;
    }

    public boolean isMayCreateTopLevelMailbox() {
        return mayCreateTopLevelMailbox;
    }
}
