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

import com.google.common.base.Preconditions;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.query.EmailQuery;

public final class StandardQueries {

    private static final Comparator[] SORT_DEFAULT =
            new Comparator[] {new Comparator(Email.Property.RECEIVED_AT, false)};

    private StandardQueries() {}

    public static EmailQuery mailbox(final IdentifiableMailboxWithRole mailbox) {
        return mailbox(mailbox.getId());
    }

    public static EmailQuery mailbox(final String mailboxId) {
        Preconditions.checkNotNull(mailboxId);
        return EmailQuery.of(
                EmailFilterCondition.builder().inMailbox(mailboxId).build(), SORT_DEFAULT, true);
    }

    public static EmailQuery keyword(final String keyword, final String[] trashAndJunk) {
        Preconditions.checkNotNull(keyword);
        Preconditions.checkNotNull(trashAndJunk);
        Preconditions.checkArgument(
                trashAndJunk.length <= 2, "Provide mailbox ids for trash and junk");
        // TODO; we probably want to change this to someInThreadHaveKeyword?
        return EmailQuery.of(
                EmailFilterCondition.builder()
                        .hasKeyword(keyword)
                        .inMailboxOtherThan(trashAndJunk)
                        .build(),
                SORT_DEFAULT,
                true);
    }

    public static EmailQuery search(final String searchTerm, final String[] trashAndJunk) {
        Preconditions.checkNotNull(searchTerm);
        Preconditions.checkNotNull(trashAndJunk);
        Preconditions.checkArgument(
                trashAndJunk.length <= 2, "Provide mailbox ids for trash and junk");
        return EmailQuery.of(
                EmailFilterCondition.builder()
                        .text(searchTerm)
                        .inMailboxOtherThan(trashAndJunk)
                        .build(),
                SORT_DEFAULT,
                true);
    }
}
