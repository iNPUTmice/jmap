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

import com.google.common.io.Resources;
import com.google.common.util.concurrent.ListenableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.api.MethodResponseNotFoundException;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.instanceOf;

public class HttpJmapClientTest {

    private static String USERNAME = "test@example.com";
    private static String PASSWORD = "secret";

    @Test
    public void fetchMailboxes() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/02-mailboxes.json")));
        server.start();

        final JmapClient jmapClient = new JmapClient(
                USERNAME,
                PASSWORD,
                server.url(".well-know/jmap").url()
        );


        final ListenableFuture<MethodResponses> future = jmapClient.call(new GetMailboxMethodCall());


        final GetMailboxMethodResponse mailboxResponse = future.get().getMain(GetMailboxMethodResponse.class);

        Assert.assertEquals(7, mailboxResponse.getList().length);

        server.shutdown();
    }

    private static String readResourceAsString(String filename) throws IOException {
        return Resources.asCharSource(Resources.getResource(filename), Charset.defaultCharset()).read().trim();
    }

    @Test
    public void fetchMailboxesWithMethodError() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/unknown-method.json")));
        server.start();

        final JmapClient jmapClient = new JmapClient(
                USERNAME,
                PASSWORD,
                server.url(".well-know/jmap").url()
        );


        ListenableFuture<MethodResponses> future = jmapClient.call(new GetMailboxMethodCall());


        try {
            future.get();
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            Assert.assertThat(cause, instanceOf(MethodErrorResponseException.class));
            final MethodErrorResponseException methodErrorResponseException = (MethodErrorResponseException) cause;
            Assert.assertThat(methodErrorResponseException.getMethodErrorResponse(), CoreMatchers.<MethodErrorResponse>instanceOf(UnknownMethodMethodErrorResponse.class));
        }

        server.shutdown();
    }

    @Test
    public void fetchMailboxesGarbage() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/unknown-method-call-id.json")));
        server.start();

        final JmapClient jmapClient = new JmapClient(
                USERNAME,
                PASSWORD,
                server.url(".well-know/jmap").url()
        );

        final ListenableFuture<MethodResponses> future = jmapClient.call(new GetMailboxMethodCall());

        try {
            future.get();
        } catch (ExecutionException e) {
            Assert.assertThat(e.getCause(), CoreMatchers.<Throwable>instanceOf(MethodResponseNotFoundException.class));
        }
    }

}
