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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithKeywords;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

/**
 * This patches MockMailServer to ignore the resultReference in Mailbox/get. This leads to
 * GetMailbox always returning all mailboxes. Even for calls triggered by changes.created and
 * changes.updated This means when processing the update on the client side we will find mailboxes
 * returned that are not references by the result of Mailbox/changes
 */
public class BrokenMailboxChangesTest {

    @Test
    public void errorInSubsequentGetCallsToChangesTriggerIllegalState()
            throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        final MyMockMailServer myMockMailServer = new MyMockMailServer(2);
        server.setDispatcher(myMockMailServer);
        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(myMockMailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(myMockMailServer.getAccountId())
                        .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            final List<IdentifiableEmailWithKeywords> emails =
                    Arrays.asList(
                            new MyIdentifiableEmailWithKeywords("M1"),
                            new MyIdentifiableEmailWithKeywords("M2"));
            mua.setKeyword(emails, Keyword.SEEN).get();

            final ExecutionException exception =
                    Assertions.assertThrows(ExecutionException.class, () -> mua.refresh().get());
            MatcherAssert.assertThat(
                    exception.getCause(), CoreMatchers.instanceOf(IllegalStateException.class));
        }
        server.shutdown();
    }

    private static class MyIdentifiableEmailWithKeywords implements IdentifiableEmailWithKeywords {

        private final String id;

        private MyIdentifiableEmailWithKeywords(String id) {
            this.id = id;
        }

        @Override
        public Map<String, Boolean> getKeywords() {
            return Collections.emptyMap();
        }

        @Override
        public String getId() {
            return id;
        }
    }

    private static class MyMockMailServer extends MockMailServer {

        public MyMockMailServer(int numThreads) {
            super(numThreads);
        }

        @Override
        protected MethodResponse[] execute(
                GetMailboxMethodCall methodCall,
                ListMultimap<String, Response.Invocation> previousResponses) {
            return new MethodResponse[] {
                GetMailboxMethodResponse.builder()
                        .list(
                                mailboxes.values().stream()
                                        .map(
                                                mailboxInfo ->
                                                        Mailbox.builder()
                                                                .id(mailboxInfo.getId())
                                                                .name(mailboxInfo.getName())
                                                                .role(mailboxInfo.getRole())
                                                                .build())
                                        .toArray(Mailbox[]::new))
                        .state(getState())
                        .build()
            };
        }
    }
}
