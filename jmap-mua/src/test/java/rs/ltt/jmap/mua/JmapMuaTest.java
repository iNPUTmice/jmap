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

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.api.ErrorResponseException;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.ErrorType;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.StubMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;

public class JmapMuaTest {

    @Test
    public void oneInboxMailbox() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final EmailServer emailServer = new EmailServer();
        server.setDispatcher(emailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final Mua mua =
                Mua.builder()
                        .cache(myInMemoryCache)
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(emailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(emailServer.getAccountId())
                        .build();
        mua.refreshMailboxes().get();
        final Mailbox mailbox = Iterables.getFirst(myInMemoryCache.getMailboxes(), null);
        Assertions.assertNotNull(mailbox);
        Assertions.assertEquals(mailbox.getRole(), Role.INBOX);
        server.shutdown();
    }

    @Test
    public void methodNotFound() throws IOException {
        final MockWebServer server = new MockWebServer();
        final EmailServer emailServer = new EmailServer();
        server.setDispatcher(emailServer);

        final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(emailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(emailServer.getAccountId())
                        .build();
        final ExecutionException executionException =
                Assertions.assertThrows(
                        ExecutionException.class, () -> mua.refreshIdentities().get());
        MatcherAssert.assertThat(
                executionException.getCause(),
                CoreMatchers.instanceOf(MethodErrorResponseException.class));
        server.shutdown();
    }

    @Test
    public void errorResponse() throws IOException {
        final MockWebServer server = new MockWebServer();
        final UnknownCapabilityMailServer emailServer = new UnknownCapabilityMailServer();
        server.setDispatcher(emailServer);

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(emailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(emailServer.getAccountId())
                        .build()) {
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> mua.refreshIdentities().get());
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(ErrorResponseException.class));

            final ErrorResponseException errorResponseException =
                    (ErrorResponseException) executionException.getCause();
            Assertions.assertEquals(
                    ErrorType.UNKNOWN_CAPABILITY,
                    errorResponseException.getErrorResponse().getType());
        }
        server.shutdown();
    }

    @Test
    public void unauthorized() throws IOException {
        final MockWebServer server = new MockWebServer();
        final EmailServer emailServer = new EmailServer();
        server.setDispatcher(emailServer);

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(emailServer.getUsername())
                        .password("wrong")
                        .accountId(emailServer.getAccountId())
                        .build()) {
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> mua.refreshIdentities().get());
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(UnauthorizedException.class));
        }
        server.shutdown();
    }

    @Test
    public void invalidSessionResourceEmpty() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{}").setResponseCode(200));

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username("irrelevant")
                        .password("wrong")
                        .accountId("irrelevant")
                        .build()) {
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> mua.refreshIdentities().get());
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(InvalidSessionResourceException.class));
        }
        server.shutdown();
    }

    @Test
    public void invalidSessionResourceJson() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{]").setResponseCode(200));

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username("irrelevant")
                        .password("wrong")
                        .accountId("irrelevant")
                        .build()) {
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> mua.refreshIdentities().get());
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(InvalidSessionResourceException.class));
            executionException.printStackTrace();
        }
        server.shutdown();
    }

    private static class EmailServer extends StubMailServer {
        @Override
        protected MethodResponse[] execute(
                final GetMailboxMethodCall methodCall,
                final ListMultimap<String, Response.Invocation> previousResponses) {
            return new MethodResponse[] {
                GetMailboxMethodResponse.builder()
                        .list(
                                new Mailbox[] {
                                    Mailbox.builder().name("Inbox").role(Role.INBOX).build()
                                })
                        .accountId(getAccountId())
                        .build()
            };
        }
    }

    private static class UnknownCapabilityMailServer extends StubMailServer {

        @Override
        protected GenericResponse dispatch(final Request request) {
            return new ErrorResponse(ErrorType.UNKNOWN_CAPABILITY, 400);
        }
    }

    private static class MyInMemoryCache extends InMemoryCache {
        public Collection<Mailbox> getMailboxes() {
            return this.mailboxes.values();
        }
    }
}
