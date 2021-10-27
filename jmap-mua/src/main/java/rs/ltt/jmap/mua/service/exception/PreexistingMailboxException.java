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

package rs.ltt.jmap.mua.service.exception;

import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;
import rs.ltt.jmap.common.entity.Role;

public class PreexistingMailboxException extends IllegalStateException {

    private final IdentifiableMailboxWithRoleAndName preexistingMailbox;
    private final Role targetRole;

    public PreexistingMailboxException(
            IdentifiableMailboxWithRoleAndName preexistingMailbox, final Role role) {
        super(
                String.format(
                        "A mailbox by the name %s already exits", preexistingMailbox.getName()));
        this.preexistingMailbox = preexistingMailbox;
        this.targetRole = role;
    }

    public IdentifiableMailboxWithRoleAndName getPreexistingMailbox() {
        return this.preexistingMailbox;
    }

    public Role getTargetRole() {
        return this.targetRole;
    }

    public static Void throwIfNotNull(
            final IdentifiableMailboxWithRoleAndName mailbox, final Role role) {
        if (mailbox != null) {
            throw new PreexistingMailboxException(mailbox, role);
        }
        return null;
    }
}
