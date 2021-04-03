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

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.*;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.FilterOperator;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MockMailServerTest {


    @Test
    public void queryRefreshQuery() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(128);
        //this will make the second query() a queryChangesCall
        mailServer.setReportCanCalculateQueryChanges(true);
        server.setDispatcher(mailServer);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build()) {
            final EmailQuery query = EmailQuery.of(
                    FilterOperator.or(
                            EmailFilterCondition.builder().inMailbox("0").build(),
                            EmailFilterCondition.builder().inMailbox("1").build()
                    ),
                    true
            );
            mua.query(query).get();
            mua.refresh().get();
            mua.query(query).get();
        }
        server.shutdown();
    }

    @Test
    public void addEmailAndRefresh() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(128);
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
            final Email email = mailServer.generateEmailOnTop();
            final Status status = mua.refresh().get();
            Assertions.assertEquals(Status.UPDATED, status);
            Assertions.assertTrue(cache.getEmailIds().contains(email.getId()), "new email id not found in cache");
            Assertions.assertTrue(cache.getThreadIds().contains(email.getThreadId()), "new thread id not found in cache");
        }
        server.shutdown();
    }

    @Test
    public void queryAndMarkAsRead() throws IOException, ExecutionException, InterruptedException {
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
            final Mailbox mailboxBeforeModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(2, mailboxBeforeModification.getUnreadThreads(), "Miss match in unread threads");
            Assertions.assertEquals(3, mailboxBeforeModification.getUnreadEmails(), "Miss match in unread emails");
            final List<CachedEmail> emails = cache.getEmails("T1");
            mua.setKeyword(emails, Keyword.SEEN).get();

            mua.refresh().get();

            final Mailbox mailboxAfterModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(
                    1,
                    mailboxAfterModification.getUnreadThreads(),
                    "Miss match in unread thread after modification"
            );
            Assertions.assertEquals(
                    1,
                    mailboxAfterModification.getUnreadEmails(),
                    "Miss match in unread email after modification"
            );
        }
        server.shutdown();
    }

    @Test
    public void queryAndRemoveFromInbox() throws ExecutionException, InterruptedException {
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
            final List<CachedEmail> threadT1 = cache.getEmails("T1");
            mua.archive(threadT1).get();
            //creating the archive mailbox and adding email are two steps / two versions
            Assertions.assertEquals(Status.HAS_MORE, mua.refresh().get());
            Assertions.assertEquals(Status.UPDATED, mua.refresh().get());

            final Mailbox inboxAfterModification = cache.getMailbox(Role.INBOX);
            final Mailbox archiveAfterModification = cache.getMailbox(Role.ARCHIVE);

            Assertions.assertNotNull(archiveAfterModification);

            Assertions.assertEquals(1, archiveAfterModification.getUnreadThreads());
            Assertions.assertEquals(1, inboxAfterModification.getUnreadThreads());

            Assertions.assertEquals(2, archiveAfterModification.getTotalEmails());
            Assertions.assertEquals(1, inboxAfterModification.getTotalEmails());

            final List<CachedEmail> threadT0 = cache.getEmails("T0");

            mua.archive(threadT0).get();

            mua.refresh().get();

            final Mailbox inboxAfterSecondModification = cache.getMailbox(Role.INBOX);
            final Mailbox archiveAfterSecondModification = cache.getMailbox(Role.ARCHIVE);
            Assertions.assertEquals(0, inboxAfterSecondModification.getTotalEmails());
            Assertions.assertEquals(3, archiveAfterSecondModification.getTotalEmails());

        }
    }

}
