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

package rs.ltt.jmap.common.entity;

import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Mailbox extends AbstractIdentifiableEntity implements IdentifiableMailboxWithRole {

    private String name;

    private String parentId;

    private Role role;

    private Long sortOrder;

    private Long totalEmails;

    private Long unreadEmails;

    private Long totalThreads;

    private Long unreadThreads;

    private MailboxRights myRights;

    private Boolean isSubscribed;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("parentId", parentId)
                .add("role", role)
                .add("sortOrder", sortOrder)
                .add("totalEmails", totalEmails)
                .add("unreadEmails", unreadEmails)
                .add("totalThreads", totalThreads)
                .add("unreadThreads", unreadThreads)
                .add("myRights", myRights)
                .add("isSubscribed", isSubscribed)
                .add("id", id)
                .toString();
    }
}
