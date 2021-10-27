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

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

public class GetEmailsOutOfOrderTest {

    @Test
    public void emailGetIsOutOfOrder()
            throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new GetEmailOutOfOrder(2);
        server.setDispatcher(mailServer);
        final MyInMemoryCache cache = new MyInMemoryCache();
        try (final Mua mua =
                Mua.builder()
                        .cache(cache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getUsername())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            final Mailbox mailboxBeforeModification = cache.getMailbox(Role.INBOX);
            Assertions.assertEquals(
                    2,
                    mailboxBeforeModification.getUnreadThreads(),
                    "Miss match in unread threads");
            Assertions.assertEquals(
                    3, mailboxBeforeModification.getUnreadEmails(), "Miss match in unread emails");
            final List<CachedEmail> emails = cache.getEmails("T1");
            mua.setKeyword(emails, Keyword.SEEN).get();

            mua.refresh().get();
        }
        server.shutdown();
    }

    private static class GetEmailOutOfOrder extends MockMailServer {

        public GetEmailOutOfOrder(int numThreads) {
            super(numThreads);
        }

        @Override
        protected MethodResponse[] execute(
                GetEmailMethodCall methodCall,
                ListMultimap<String, Response.Invocation> previousResponses) {
            final MethodResponse[] response = super.execute(methodCall, previousResponses);
            GetEmailMethodResponse getEmailMethodResponse = (GetEmailMethodResponse) response[0];
            return new MethodResponse[] {
                GetEmailMethodResponse.builder()
                        .list(
                                Lists.reverse(Arrays.asList(getEmailMethodResponse.getList()))
                                        .toArray(new Email[0]))
                        .state(getState())
                        .build()
            };
        }
    }
}
