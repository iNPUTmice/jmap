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
import com.google.common.collect.ListMultimap;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.util.MailboxUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModifyLabelsTest {

    final static IdentifiableMailboxWithRoleAndName INBOX_WITH_ID = new StubMailbox("1", Role.INBOX);
    final static IdentifiableMailboxWithRoleAndName INBOX = new StubMailbox(null, Role.INBOX);
    final static IdentifiableMailboxWithRoleAndName JMAP_WITH_ID = new StubMailbox("2", null, "JMAP");
    final static IdentifiableMailboxWithRoleAndName JMAP = new StubMailbox(null, null, "JMAP");
    final static IdentifiableMailboxWithRoleAndName ARCHIVE_WITH_ID = new StubMailbox("3", Role.ARCHIVE);
    final static IdentifiableMailboxWithRoleAndName ARCHIVE = new StubMailbox(null, Role.ARCHIVE);

    @Test
    public void removeNonIdentifiable() {
        Collection<IdentifiableEmailWithMailboxIds> emails = Collections.singleton(
                Email.builder().mailboxId(INBOX_WITH_ID.getId(), true).build()
        );
        try (final Mua mua = Mua.builder()
                .username("ignored")
                .password(JmapDispatcher.PASSWORD)
                .accountId("ignored")
                .build()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> mua.modifyLabels(
                    emails,
                    Collections.emptyList(),
                    ImmutableList.of(ARCHIVE)
            ).get());
        }
    }

    @Test
    public void simultaneousAdditionAndRemoval() {
        Collection<IdentifiableEmailWithMailboxIds> emails = Collections.singleton(
                Email.builder().mailboxId(INBOX_WITH_ID.getId(), true).build()
        );
        try (final Mua mua = Mua.builder()
                .username("ignored")
                .password(JmapDispatcher.PASSWORD)
                .accountId("ignored")
                .build()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> mua.modifyLabels(
                    emails,
                    ImmutableList.of(INBOX),
                    ImmutableList.of(INBOX_WITH_ID)
            ).get());
            Assertions.assertThrows(IllegalArgumentException.class, () -> mua.modifyLabels(
                    emails,
                    ImmutableList.of(INBOX, JMAP_WITH_ID),
                    ImmutableList.of(INBOX_WITH_ID)
            ).get());
            Assertions.assertThrows(IllegalArgumentException.class, () -> mua.modifyLabels(
                    emails,
                    ImmutableList.of(INBOX_WITH_ID, JMAP),
                    ImmutableList.of(JMAP_WITH_ID)
            ));
            mua.modifyLabels(
                    emails,
                    ImmutableList.of(INBOX_WITH_ID, JMAP),
                    ImmutableList.of(ARCHIVE_WITH_ID)
            );
        }
    }


    @Test
    public void archiveEquivalent() throws ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua = Mua.builder()
                .cache(cache)
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build()) {
            mua.query(EmailQuery.unfiltered()).get();

            final Mailbox inbox = cache.getMailbox(Role.INBOX);

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            mua.modifyLabels(threadT1, Collections.emptyList(), ImmutableList.of(inbox)).get();

            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());

            final Mailbox inboxAfterModification = cache.getMailbox(Role.INBOX);
            final Mailbox archiveAfterModification = cache.getMailbox(Role.ARCHIVE);

            Assertions.assertNotNull(archiveAfterModification);

            Assertions.assertEquals(1, archiveAfterModification.getUnreadThreads());
            Assertions.assertEquals(1, inboxAfterModification.getUnreadThreads());

            Assertions.assertEquals(2, archiveAfterModification.getTotalEmails());
            Assertions.assertEquals(1, inboxAfterModification.getTotalEmails());

        }
    }

    @Test
    public void addToLabelJmap() throws ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua = Mua.builder()
                .cache(cache)
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build()) {
            mua.query(EmailQuery.unfiltered()).get();

            final Mailbox inbox = cache.getMailbox(Role.INBOX);

            Assertions.assertEquals(2, inbox.getUnreadThreads());
            Assertions.assertEquals(3, inbox.getTotalEmails());

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            mua.modifyLabels(threadT1, ImmutableList.of(JMAP), Collections.emptyList()).get();

            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());

            final Mailbox inboxAfterModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(2, inboxAfterModification.getUnreadThreads());
            Assertions.assertEquals(3, inboxAfterModification.getTotalEmails());

            final Mailbox jmap = cache.getMailboxes().stream().filter(mailbox -> mailbox.getName().equals("JMAP")).findFirst().orElse(null);
            Assertions.assertNotNull(jmap);

            Assertions.assertEquals(1, jmap.getTotalThreads());


        }
    }

    @Test
    public void ensureIfInStateIsSet() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final AtomicBoolean ifInState = new AtomicBoolean(false);
        final MockMailServer mailServer = new MockMailServer(2) {
            @Override
            protected List<MailboxInfo> generateMailboxes() {
                return Arrays.asList(
                        new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                        new MailboxInfo(UUID.randomUUID().toString(), "JMAP", null),
                        new MailboxInfo(UUID.randomUUID().toString(), "Archive", Role.ARCHIVE)
                );
            }

            @Override
            protected MethodResponse[] execute(SetEmailMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
                if (Objects.nonNull(methodCall.getIfInState())) {
                    ifInState.set(true);
                }
                return super.execute(methodCall, previousResponses);
            }
        };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua = Mua.builder()
                .cache(cache)
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            //just reconfirming that mock server is setup correctly
            final Mailbox inbox = cache.getMailbox(Role.INBOX);
            Assertions.assertNotNull(inbox);
            Assertions.assertEquals(2, inbox.getUnreadThreads());
            Assertions.assertEquals(3, inbox.getTotalEmails());

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            mua.modifyLabels(threadT1, ImmutableList.of(JMAP, inbox), Collections.emptyList()).get();

            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());

            final Mailbox jmap = cache.getMailboxes().stream().filter(mailbox -> mailbox.getName().equals("JMAP")).findFirst().orElse(null);
            Assertions.assertNotNull(jmap);

            Assertions.assertEquals(1, jmap.getTotalThreads());


            Assertions.assertTrue(ifInState.get(),"If in state had not been set");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void addToExistingJmapLabel() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2) {
            @Override
            protected List<MailboxInfo> generateMailboxes() {
                return Arrays.asList(
                        new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                        new MailboxInfo(UUID.randomUUID().toString(), "JMAP", null)
                );
            }
        };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua = Mua.builder()
                .cache(cache)
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            //just reconfirming that mock server is setup correctly
            final Mailbox inbox = cache.getMailbox(Role.INBOX);
            Assertions.assertNotNull(inbox);
            Assertions.assertEquals(2, inbox.getUnreadThreads());
            Assertions.assertEquals(3, inbox.getTotalEmails());

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            mua.modifyLabels(threadT1, ImmutableList.of(JMAP, inbox), Collections.emptyList()).get();

            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());

            final Mailbox jmap = cache.getMailboxes().stream().filter(mailbox -> mailbox.getName().equals("JMAP")).findFirst().orElse(null);
            Assertions.assertNotNull(jmap);

            Assertions.assertEquals(1, jmap.getTotalThreads());


        } finally {
            server.shutdown();
        }
    }

    public static final class StubMailbox implements IdentifiableMailboxWithRoleAndName {
        private final String id;
        private final Role role;
        private final String name;

        public StubMailbox(String id, Role role) {
            this.id = id;
            this.role = role;
            this.name = MailboxUtil.humanReadable(role);
        }

        public StubMailbox(String id, Role role, String name) {
            this.id = id;
            this.role = role;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Role getRole() {
            return role;
        }

        @Override
        public String getId() {
            return id;
        }
    }

}
