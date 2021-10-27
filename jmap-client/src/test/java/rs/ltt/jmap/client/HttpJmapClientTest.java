/*
 * Copyright 2019 Daniel Gultsch
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

package rs.ltt.jmap.client;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.api.MethodResponseNotFoundException;
import rs.ltt.jmap.client.event.CloseAfter;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.client.session.InMemorySessionCache;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.capability.WebSocketCapability;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.error.InvalidArgumentsMethodErrorResponse;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;

public class HttpJmapClientTest {

    public static final String WELL_KNOWN_PATH = ".well-known/jmap";
    private static final String ACCOUNT_ID = "test@example.com";
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "secret";
    @TempDir File tempDir;

    @Test
    public void fetchMailboxes() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/02-mailboxes.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ListenableFuture<MethodResponses> future =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final GetMailboxMethodResponse mailboxResponse =
                future.get().getMain(GetMailboxMethodResponse.class);

        Assertions.assertEquals(7, mailboxResponse.getList().length);

        server.shutdown();
    }

    public static String readResourceAsString(String filename) throws IOException {
        return Resources.asCharSource(Resources.getResource(filename), Charsets.UTF_8)
                .read()
                .trim();
    }

    @Test
    public void repeatedSessionFetches() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ListenableFuture<Session> firstSessionFuture = jmapClient.getSession();
        final ListenableFuture<Session> secondSessionFuture = jmapClient.getSession();

        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(new MockResponse().setResponseCode(404));

        final Session firstSession = firstSessionFuture.get();
        final Session secondSession = secondSessionFuture.get();
        Assertions.assertEquals("/jmap/", firstSession.getApiUrl().encodedPath());
        Assertions.assertEquals("/jmap/", secondSession.getApiUrl().encodedPath());

        final ListenableFuture<Session> thirdSessionFuture = jmapClient.getSession();
        Assertions.assertEquals("/jmap/", thirdSessionFuture.get().getApiUrl().encodedPath());

        server.shutdown();
    }

    @Test
    public void fileSessionCache() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.start();

        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(new MockResponse().setResponseCode(404));

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));
        jmapClient.setSessionCache(new FileSessionCache(tempDir));

        final ListenableFuture<Session> firstSessionFuture = jmapClient.getSession();
        final Session firstSession = firstSessionFuture.get();
        Assertions.assertEquals("/jmap/", firstSession.getApiUrl().encodedPath());

        final JmapClient jmapClient2 =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));
        jmapClient2.setSessionCache(new FileSessionCache(tempDir));

        final ListenableFuture<Session> secondSessionFuture = jmapClient2.getSession();

        final Session secondSession = secondSessionFuture.get();
        Assertions.assertEquals("/jmap/", secondSession.getApiUrl().encodedPath());

        final ListenableFuture<Session> thirdSessionFuture = jmapClient2.getSession();
        Assertions.assertEquals("/jmap/", thirdSessionFuture.get().getApiUrl().encodedPath());

        server.shutdown();
    }

    @Test
    public void fetchMailboxesWithMethodError() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/unknown-method.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        ListenableFuture<MethodResponses> future =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final ExecutionException exception =
                Assertions.assertThrows(ExecutionException.class, future::get);
        final Throwable cause = exception.getCause();
        MatcherAssert.assertThat(
                cause, CoreMatchers.instanceOf(MethodErrorResponseException.class));
        final MethodErrorResponseException methodErrorResponseException =
                (MethodErrorResponseException) cause;
        MatcherAssert.assertThat(
                methodErrorResponseException.getMethodErrorResponse(),
                CoreMatchers.instanceOf(UnknownMethodMethodErrorResponse.class));
        Assertions.assertEquals(
                "unknownMethod in response to Mailbox/get",
                methodErrorResponseException.getMessage());
        Assertions.assertEquals(0, methodErrorResponseException.getAdditional().length);
        server.shutdown();
    }

    @Test
    public void invalidArgumentsMethodError() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/invalid-arguments.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ExecutionException exception =
                Assertions.assertThrows(
                        ExecutionException.class,
                        () ->
                                jmapClient
                                        .call(
                                                GetMailboxMethodCall.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .build())
                                        .get());
        final Throwable cause = exception.getCause();
        MatcherAssert.assertThat(
                cause, CoreMatchers.instanceOf(MethodErrorResponseException.class));
        final MethodErrorResponseException methodErrorResponseException =
                (MethodErrorResponseException) cause;
        MatcherAssert.assertThat(
                methodErrorResponseException.getMethodErrorResponse(),
                CoreMatchers.instanceOf(InvalidArgumentsMethodErrorResponse.class));
        Assertions.assertEquals(
                "invalidArguments in response to Mailbox/get (I provide more details)",
                methodErrorResponseException.getMessage());
        server.shutdown();
    }

    @Test
    public void fetchMailboxesException() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(
                                readResourceAsString(
                                        "fetch-mailboxes/unknown-method-call-id.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ExecutionException exception =
                Assertions.assertThrows(
                        ExecutionException.class,
                        () -> {
                            jmapClient
                                    .call(
                                            GetMailboxMethodCall.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .build())
                                    .get();
                        });
        MatcherAssert.assertThat(
                exception.getCause(),
                CoreMatchers.instanceOf(MethodResponseNotFoundException.class));
        server.shutdown();
    }

    @Test
    public void endpointNotFound() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(404));

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ExecutionException exception =
                Assertions.assertThrows(
                        ExecutionException.class,
                        () ->
                                jmapClient
                                        .call(
                                                EchoMethodCall.builder()
                                                        .libraryName(Version.getUserAgent())
                                                        .build())
                                        .get());

        MatcherAssert.assertThat(
                exception.getCause(), CoreMatchers.instanceOf(EndpointNotFoundException.class));

        server.shutdown();
    }

    @Test
    public void updateSessionResourceIfNecessary()
            throws IOException, InterruptedException, ExecutionException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("update-session-resource/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(
                                readResourceAsString("update-session-resource/02-mailboxes.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("update-session-resource/03-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("update-session-resource/04-echo.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ListenableFuture<MethodResponses> mailboxFuture =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        // Wait for result
        mailboxFuture.get();

        // Skip session request
        server.takeRequest();

        Assertions.assertEquals(server.url("/jmap/"), server.takeRequest().getRequestUrl());

        final ListenableFuture<MethodResponses> echoFuture =
                jmapClient.call(EchoMethodCall.builder().build());

        // Wait for result
        echoFuture.get();

        // Skip session request
        server.takeRequest();

        Assertions.assertEquals(server.url("/api/jmap/"), server.takeRequest().getRequestUrl());

        server.shutdown();
    }

    @Test
    public void useStoredSessionResource()
            throws IOException, ExecutionException, InterruptedException {
        final AtomicInteger cacheReadAttempts = new AtomicInteger();
        final AtomicInteger cacheHits = new AtomicInteger();
        final InMemorySessionCache sessionCache =
                new InMemorySessionCache() {
                    @Override
                    public ListenableFuture<Session> load(
                            final String username, final HttpUrl sessionResource) {
                        cacheReadAttempts.incrementAndGet();
                        final ListenableFuture<Session> future =
                                super.load(username, sessionResource);
                        try {
                            if (future.get() != null) {
                                cacheHits.incrementAndGet();
                            }
                        } catch (Exception e) {
                            // ignored
                        }
                        return future;
                    }
                };
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/02-mailboxes.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/02-mailboxes.json")));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));
        jmapClient.setSessionCache(sessionCache);
        jmapClient.getSession().get();

        final JmapClient secondJmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));
        secondJmapClient.setSessionCache(sessionCache);

        final ListenableFuture<MethodResponses> firstFuture =
                secondJmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final GetMailboxMethodResponse firstMailboxResponse =
                firstFuture.get().getMain(GetMailboxMethodResponse.class);

        Assertions.assertEquals(7, firstMailboxResponse.getList().length);

        final ListenableFuture<MethodResponses> secondFuture =
                secondJmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final GetMailboxMethodResponse secondMailboxResponse =
                secondFuture.get().getMain(GetMailboxMethodResponse.class);

        Assertions.assertEquals(7, secondMailboxResponse.getList().length);

        Assertions.assertEquals(
                2, cacheReadAttempts.get(), "Unexpected number of session cache read attempts");

        Assertions.assertEquals(
                1, cacheHits.get(), "Unexpected number of session cache read attempts");

        server.shutdown();
    }

    @Test
    public void redirectFromWellKnown()
            throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(301).addHeader("Location", "/jmap"));
        server.enqueue(new MockResponse().setResponseCode(301).addHeader("Location", "/jmap/"));
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("redirect/01-session.json")));

        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        Session session = jmapClient.getSession().get();

        Assertions.assertEquals(server.url("/jmap/"), session.getBase());

        server.shutdown();
    }

    @Test
    public void downloadUploadAndEventSourceUrlTest()
            throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("session-urls/01-session.json")));
        server.start();
        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final Session session = jmapClient.getSession().get();

        HttpUrl download = session.getDownloadUrl(USERNAME, "B10B1D", "lttrs", "text/plain");
        HttpUrl upload = session.getUploadUrl(USERNAME);
        HttpUrl eventSource =
                session.getEventSourceUrl(
                        Arrays.asList(Email.class, Mailbox.class), CloseAfter.STATE, 300L);

        Assertions.assertEquals(
                server.url("/jmap/download/test%40example.com/B10B1D/lttrs?accept=text%2Fplain"),
                download);
        Assertions.assertEquals(server.url("/jmap/upload/test%40example.com/"), upload);
        Assertions.assertEquals(
                server.url("jmap/eventsource/?types=Email,Mailbox&closeafter=state&ping=300"),
                eventSource);

        server.shutdown();
    }

    @Test
    public void incompleteSessionResource()
            throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("broken-session-urls/01-session.json")));
        server.start();
        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final Session session = jmapClient.getSession().get();
        Assertions.assertThrows(IllegalStateException.class, () -> session.getUploadUrl(USERNAME));
        server.shutdown();
    }

    @Test
    public void webSocketUrl() throws IOException, ExecutionException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("session-urls/02-session-ws.json")));
        server.start();
        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final Session session = jmapClient.getSession().get();
        Assertions.assertNotNull(session.getCapability(WebSocketCapability.class).getUrl());
    }

    @Test
    public void invalidJsonResponse() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("session-urls/01-session.json")));
        server.enqueue(new MockResponse().setBody("Garbage"));
        server.start();
        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));
        final ListenableFuture<MethodResponses> future =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final ExecutionException executionException =
                Assertions.assertThrows(
                        ExecutionException.class,
                        () -> future.get().getMain(GetMailboxMethodResponse.class));

        MatcherAssert.assertThat(
                executionException.getCause(), CoreMatchers.instanceOf(JsonParseException.class));

        server.shutdown();
    }

    @Test
    public void callIsCancelableSession() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json"))
                        .setSocketPolicy(SocketPolicy.STALL_SOCKET_AT_START));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ListenableFuture<MethodResponses> future =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final Dispatcher dispatcher = Services.OK_HTTP_CLIENT.dispatcher();
        future.cancel(true);
        Thread.sleep(1000); // wait for cancel to propagate.
        Assertions.assertEquals(
                0,
                dispatcher.runningCallsCount() + dispatcher.queuedCallsCount(),
                "Call has not been cancelled");
        server.shutdown();
    }

    @Test
    public void callIsCancelableRequest() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(readResourceAsString("fetch-mailboxes/02-mailboxes.json"))
                        .throttleBody(1, 1, TimeUnit.SECONDS));
        server.start();

        final JmapClient jmapClient =
                new JmapClient(USERNAME, PASSWORD, server.url(WELL_KNOWN_PATH));

        final ListenableFuture<MethodResponses> future =
                jmapClient.call(GetMailboxMethodCall.builder().accountId(ACCOUNT_ID).build());

        final Dispatcher dispatcher = Services.OK_HTTP_CLIENT.dispatcher();

        Thread.sleep(1000);
        future.cancel(true);
        Thread.sleep(1000); // wait for cancel to propagate

        Assertions.assertEquals(
                0,
                dispatcher.runningCallsCount() + dispatcher.queuedCallsCount(),
                "Call has not been cancelled");
    }
}
