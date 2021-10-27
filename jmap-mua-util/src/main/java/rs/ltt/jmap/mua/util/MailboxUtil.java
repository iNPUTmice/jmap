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

package rs.ltt.jmap.mua.util;

import com.google.common.base.CaseFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithMailboxIds;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;

public class MailboxUtil {

    /**
     * when auto creating mailboxes we pick a 'human readable' version of the role as the name.
     * Since mailbox names have to be unique it is not advisable (although not prohibited by the
     * standard) to use those names when creating 'labels'
     */
    public static List<String> RESERVED_MAILBOX_NAMES =
            Arrays.stream(Role.values())
                    .map(MailboxUtil::humanReadable)
                    .collect(Collectors.toList());

    public static @Nullable IdentifiableMailboxWithRole find(
            Collection<? extends IdentifiableMailboxWithRole> mailboxes, Role role) {
        for (IdentifiableMailboxWithRole mailbox : mailboxes) {
            if (mailbox.getRole() == role) {
                return mailbox;
            }
        }
        return null;
    }

    public static boolean anyWithRole(
            Collection<? extends IdentifiableMailboxWithRole> mailboxes, Role role) {
        for (final IdentifiableMailboxWithRole mailbox : mailboxes) {
            if (mailbox.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    public static Mailbox create(final Role role) {
        return Mailbox.builder().role(role).name(humanReadable(role)).build();
    }

    public static String humanReadable(final Role role) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, role.toString());
    }

    public static boolean anyIn(
            Collection<? extends IdentifiableEmailWithMailboxIds> emails, String mailboxId) {
        for (IdentifiableEmailWithMailboxIds email : emails) {
            if (email.getMailboxIds().containsKey(mailboxId)) {
                return true;
            }
        }
        return false;
    }
}
