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

package rs.ltt.jmap.mua;

import com.google.common.collect.ImmutableList;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.cache.InMemoryCache;
import rs.ltt.jmap.mua.util.MailboxUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class MyInMemoryCache extends InMemoryCache {
    public Collection<String> getEmailIds() {
        return emails.keySet();
    }

    public Collection<String> getThreadIds() {
        return threads.keySet();
    }

    public List<CachedEmail> getEmails(final String threadId) {
        List<String> emailIds = this.threads.get(threadId).getEmailIds();
        return emailIds.stream().map(id -> new CachedEmail(emails.get(id))).collect(Collectors.toList());
    }

    public Mailbox getMailbox(final Role role) {
        return (Mailbox) MailboxUtil.find(this.mailboxes.values(), role);
    }

    public List<Mailbox> getMailboxes() {
        return ImmutableList.copyOf(mailboxes.values());
    }
}
