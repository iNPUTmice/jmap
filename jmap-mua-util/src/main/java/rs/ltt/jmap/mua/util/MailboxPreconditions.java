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

package rs.ltt.jmap.mua.util;

import java.util.Collection;
import java.util.Objects;
import rs.ltt.jmap.common.entity.Identifiable;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRoleAndName;

public class MailboxPreconditions {

    public static void checkNonMatches(
            final Collection<? extends IdentifiableMailboxWithRoleAndName> a,
            final Collection<? extends IdentifiableMailboxWithRoleAndName> b) {
        for (final IdentifiableMailboxWithRoleAndName mailbox : a) {
            checkNonMatches(mailbox, b);
        }
        for (final IdentifiableMailboxWithRoleAndName mailbox : b) {
            checkNonMatches(mailbox, a);
        }
    }

    private static void checkNonMatches(
            final IdentifiableMailboxWithRoleAndName a,
            final Collection<? extends IdentifiableMailboxWithRoleAndName> b) {
        if (a.matchesAny(b)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Mailbox with role %s and name %s appears in both arguments",
                            a.getRole(), a.getName()));
        }
    }

    public static void checkAllIdentifiable(
            final Collection<? extends Identifiable> entities, final String message) {
        if (entities.stream().anyMatch(e -> Objects.isNull(e.getId()))) {
            throw new IllegalArgumentException(message);
        }
    }
}
