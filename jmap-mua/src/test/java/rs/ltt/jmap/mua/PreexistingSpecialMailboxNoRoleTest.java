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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mock.server.EmailGenerator;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.service.exception.PreexistingMailboxException;
import rs.ltt.jmap.mua.util.MailboxUtil;

public class PreexistingSpecialMailboxNoRoleTest {

    @Test
    public void archive() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Archive", null));
                    }
                };
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
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.ARCHIVE));
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.archive(threadT1).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.ARCHIVE));
            mua.archive(threadT1).get();
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void modifyLabelsRemoveInbox()
            throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Archive", null));
                    }
                };
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
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly

            final Mailbox inbox = cache.getMailbox(Role.INBOX);
            Assertions.assertNotNull(inbox);
            Assertions.assertNull(cache.getMailbox(Role.ARCHIVE));
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.modifyLabels(
                                                threadT1,
                                                Collections.emptyList(),
                                                Collections.singleton(inbox))
                                        .get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void inbox() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", null),
                                new MailboxInfo(
                                        UUID.randomUUID().toString(), "Archive", Role.ARCHIVE));
                    }

                    @Override
                    protected void generateEmail(final int numThreads, final int offset) {
                        final String mailboxId =
                                MailboxUtil.find(mailboxes.values(), Role.ARCHIVE).getId();
                        int emailCount = offset;
                        for (int thread = 0; thread < numThreads; ++thread) {
                            final int numInThread = (thread % 4) + 1;
                            for (int i = 0; i < numInThread; ++i) {
                                final Email email =
                                        EmailGenerator.get(
                                                account,
                                                mailboxId,
                                                emailCount,
                                                thread,
                                                i,
                                                numInThread);
                                this.emails.put(email.getId(), email);
                                emailCount++;
                            }
                        }
                    }
                };
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
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.INBOX));
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.moveToInbox(threadT1).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.INBOX));
            mua.archive(threadT1).get();
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void trash() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Trash", null));
                    }
                };
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
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.TRASH));
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.moveToTrash(threadT1).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.TRASH));
            mua.moveToTrash(threadT1).get();
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void important() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Important", null));
                    }
                };
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
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.IMPORTANT));
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.copyToImportant(threadT1).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.IMPORTANT));
            mua.copyToImportant(threadT1).get();
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void draft() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Drafts", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Email email = Email.builder().subject("Stub Email").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.DRAFTS));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.draft(email).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.DRAFTS));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void submit() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Sent", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Email email = Email.builder().id("non-existent").subject("Stub Email").build();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.SENT));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.submit(email, identity).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.SENT));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void submitById() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Sent", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.SENT));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.submit("i-do-not-exist", identity).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.SENT));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void sendWithPreexistingDrafts()
            throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Drafts", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Email email = Email.builder().subject("Stub Email").build();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.SENT));
            Assertions.assertNull(cache.getMailbox(Role.DRAFTS));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.send(email, identity).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.DRAFTS));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void sendWithPreexistingSent()
            throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Sent", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Email email = Email.builder().subject("Stub Email").build();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.SENT));
            Assertions.assertNull(cache.getMailbox(Role.DRAFTS));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.send(email, identity).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
            final PreexistingMailboxException preexistingMailboxException =
                    (PreexistingMailboxException) executionException.getCause();
            mua.setRole(
                            preexistingMailboxException.getPreexistingMailbox(),
                            preexistingMailboxException.getTargetRole())
                    .get();
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());
            Assertions.assertNotNull(cache.getMailbox(Role.SENT));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void sendWithPreexistingSentAndDrafts()
            throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer =
                new MockMailServer(2) {
                    @Override
                    protected List<MailboxInfo> generateMailboxes() {
                        return Arrays.asList(
                                new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                                new MailboxInfo(UUID.randomUUID().toString(), "Sent", null),
                                new MailboxInfo(UUID.randomUUID().toString(), "Drafts", null));
                    }
                };
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        final Email email = Email.builder().subject("Stub Email").build();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            // just reconfirming that mock server is setup correctly
            Assertions.assertNull(cache.getMailbox(Role.SENT));
            Assertions.assertNull(cache.getMailbox(Role.DRAFTS));
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class,
                            () -> {
                                mua.send(email, identity).get();
                            });
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(PreexistingMailboxException.class));
        } finally {
            server.shutdown();
        }
    }
}
