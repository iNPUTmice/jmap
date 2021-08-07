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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParseException;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.client.api.WebSocketClosedException;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.client.event.WebSocketPushService;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.FilterOperator;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

import java.io.IOException;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class MuaWebSocketTest {

    @Test
    public void simpleQuery() throws ExecutionException, InterruptedException, IOException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(128);
        server.setDispatcher(mailServer);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
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

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            Assertions.assertEquals(2, threadT1.size());

        }
        server.shutdown();
    }

    @Test
    public void autoClose() throws ExecutionException, InterruptedException, IOException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);


        final ListenableFuture<MethodResponses> future;
        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
                .accountId(mailServer.getAccountId())
                .build()) {
            awaitRoundTrip(mua); //this fetches the session
            mailServer.setFailureTrigger(JmapDispatcher.FailureTrigger.IGNORE);
            future = mua.getJmapClient().call(new EchoMethodCall("jmap-mua"));

        }
        final ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        MatcherAssert.assertThat(executionException.getCause(), CoreMatchers.instanceOf(SocketException.class));
        server.shutdown();
    }

    private static void awaitRoundTrip(final Mua mua) throws ExecutionException, InterruptedException {
        Assertions.assertEquals(
                "jmap-mua",
                mua.getJmapClient()
                        .call(new EchoMethodCall("jmap-mua"))
                        .get()
                        .getMain(EchoMethodResponse.class)
                        .getLibraryName()
        );
        System.out.println("got round trip");
    }

    @Test
    public void serverShutdown() throws ExecutionException, InterruptedException, IOException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);


        final ListenableFuture<MethodResponses> future;
        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
                .accountId(mailServer.getAccountId())
                .build()) {
            awaitRoundTrip(mua); //this fetches the session
            mailServer.setFailureTrigger(JmapDispatcher.FailureTrigger.IGNORE);
            future = mua.getJmapClient().call(new EchoMethodCall("jmap-mua"));
            server.shutdown();
        }
        final ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        MatcherAssert.assertThat(executionException.getCause(), CoreMatchers.instanceOf(IOException.class));
    }

    @Test
    public void simpleQueryFailsReconnectWorksClose() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        simpleQueryFailsReconnectWorks(JmapDispatcher.FailureTrigger.CLOSE, WebSocketClosedException.class);
    }

    private void simpleQueryFailsReconnectWorks(JmapDispatcher.FailureTrigger failureTrigger, Class<? extends Exception> exception) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        mailServer.setFailureTrigger(failureTrigger);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
                .accountId(mailServer.getAccountId())
                .build()) {
            final EmailQuery query = EmailQuery.of(
                    FilterOperator.or(
                            EmailFilterCondition.builder().inMailbox("0").build(),
                            EmailFilterCondition.builder().inMailbox("1").build()
                    ),
                    true
            );
            final ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> mua.query(query).get(5, TimeUnit.SECONDS));

            MatcherAssert.assertThat(executionException.getCause(), CoreMatchers.instanceOf(exception));

            mailServer.setFailureTrigger(JmapDispatcher.FailureTrigger.NONE);

            Thread.sleep(1000);

            mua.query(query).get(5, TimeUnit.SECONDS);

            final List<CachedEmail> threadT1 = cache.getEmails("T1");

            Assertions.assertEquals(2, threadT1.size());
        }

        server.shutdown();
    }

    @Test
    public void simpleQueryFailsReconnectWorksInvalid() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        simpleQueryFailsReconnectWorks(JmapDispatcher.FailureTrigger.INVALID, JsonParseException.class);
    }

    @Test
    public void pushNotifications() throws IOException, ExecutionException, InterruptedException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
                .accountId(mailServer.getAccountId())
                .build()) {

            //fetches session. starts WebSocket
            //If we don’t do that the next round trip call might race the push enable message
            awaitRoundTrip(mua);

            final AtomicInteger stateChangeCount = new AtomicInteger();

            final OnStateChangeListener changeListener = stateChange -> {
                stateChangeCount.incrementAndGet();
                return true;
            };

            final ListenableFuture<PushService> pushService = mua.getJmapClient().monitorEvents(changeListener);

            MatcherAssert.assertThat(pushService.get(), CoreMatchers.instanceOf(WebSocketPushService.class));

            awaitRoundTrip(mua);

            Assertions.assertTrue(mailServer.hasPushEnabledWebSockets(), "No WebSockets have been enabled for push");

            final Email email = mailServer.generateEmailOnTop();

            awaitRoundTrip(mua);

            Assertions.assertEquals(1, stateChangeCount.get());

            pushService.get().removeOnStateChangeListener(changeListener);

            awaitRoundTrip(mua);

            Assertions.assertFalse(mailServer.hasPushEnabledWebSockets(), "Some WebSockets have been enabled for push");

        }

        server.shutdown();
    }

    @Test
    public void setPingInterval() throws ExecutionException, InterruptedException, IOException {
        final MyInMemoryCache cache = new MyInMemoryCache();
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .cache(cache)
                .useWebSocket(true)
                .accountId(mailServer.getAccountId())
                .build()) {
            final PushService pushService = mua.getJmapClient().monitorEvents().get();
            MatcherAssert.assertThat(pushService, CoreMatchers.instanceOf(WebSocketPushService.class));
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> pushService.setPingInterval(Duration.ofMinutes(-1))
            );
            pushService.setPingInterval(Duration.ofSeconds(10));
        }
    }

}
