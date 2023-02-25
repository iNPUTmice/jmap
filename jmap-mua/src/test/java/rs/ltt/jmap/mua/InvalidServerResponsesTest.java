/*
 * Copyright 2023 Daniel Gultsch
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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.mua.service.exception.SetMailboxException;

public class InvalidServerResponsesTest {

    private static final String ACCOUNT_ID = "test@example.com";
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "secret";
    private static final String WELL_KNOWN_PATH = ".well-known/jmap";

    @Test
    public void setMailboxInvalidResponse() throws Exception {

        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody(readResourceAsString("common/01-session.json")));
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("common/02-mailboxes.json")));
        server.enqueue(
                new MockResponse()
                        .setBody(
                                readResourceAsString(
                                        "invalid-server-response/invalid-mailbox-set.json")));

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(WELL_KNOWN_PATH))
                        .username(USERNAME)
                        .password(PASSWORD)
                        .accountId(ACCOUNT_ID)
                        .build()) {
            mua.refreshMailboxes().get();
            final ListenableFuture<Boolean> future =
                    mua.createMailbox(Mailbox.builder().name("Spam").build());
            final ExecutionException executionException =
                    Assertions.assertThrows(
                            ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            MatcherAssert.assertThat(
                    executionException.getCause(),
                    CoreMatchers.instanceOf(SetMailboxException.class));
        }
        server.shutdown();
    }

    private static String readResourceAsString(String filename) throws IOException {
        return Resources.asCharSource(Resources.getResource(filename), Charsets.UTF_8)
                .read()
                .trim();
    }
}
