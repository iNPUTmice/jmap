/*
 * Copyright 2021 Daniel Gultsch
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

import java.util.List;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

public class MailboxServiceTest {

    @Test
    public void fetchMailboxesAndRefresh() throws ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.refreshMailboxes().get();
            final Mailbox inboxBeforeModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(2, inboxBeforeModification.getTotalThreads());
            Assertions.assertEquals(3, inboxBeforeModification.getTotalEmails());
            mailServer.generateEmailOnTop();
            mua.refreshMailboxes().get();
            final Mailbox inboxAfterModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(3, inboxAfterModification.getTotalThreads());
            Assertions.assertEquals(4, inboxAfterModification.getTotalEmails());
        }
    }

    @Test
    public void createMailbox() throws ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.refreshMailboxes().get();
            final List<Mailbox> mailboxes = cache.getMailboxes();
            Assertions.assertEquals(1, mailboxes.size());
            Mailbox archive = Mailbox.builder().role(Role.ARCHIVE).name("Archive").build();
            Assertions.assertEquals(Boolean.TRUE, mua.createMailbox(archive).get());
            mua.refreshMailboxes().get();
            final List<Mailbox> mailboxesAfterCreate = cache.getMailboxes();
            Assertions.assertEquals(2, mailboxesAfterCreate.size());
            Assertions.assertTrue(
                    mailboxesAfterCreate.stream()
                            .map(Mailbox::getName)
                            .anyMatch("Archive"::equals));
        }
    }
}
