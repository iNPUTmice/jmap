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

package rs.ltt.jmap.mua;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Collection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mua.cache.InMemoryCache;
import rs.ltt.jmap.mua.util.MailboxUtil;

public class ToggleImportantTest {

    private static String ACCOUNT_ID = "test@example.com";
    private static String USERNAME = "test@example.com";
    private static String PASSWORD = "secret";
    private static String WELL_KNOWN_PATH = ".well-known/jmap";

    @Test
    public void emailAlreadyImportant() throws Exception {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody(readResourceAsString("common/01-session.json")));
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("common/02-mailboxes.json")));

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(WELL_KNOWN_PATH))
                        .username(USERNAME)
                        .password(PASSWORD)
                        .accountId(ACCOUNT_ID)
                        .build()) {
            mua.refreshMailboxes().get();

            final Collection<MyIdentifiableEmailWithMailboxes> emails =
                    ImmutableSet.of(new MyIdentifiableEmailWithMailboxes("e0", "mb2"));

            Assertions.assertFalse(mua.copyToImportant(emails).get());
        }

        server.shutdown();
    }

    private static String readResourceAsString(String filename) throws IOException {
        return Resources.asCharSource(Resources.getResource(filename), Charsets.UTF_8)
                .read()
                .trim();
    }

    @Test
    public void emailAlreadyNotImportant() throws Exception {
        final MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody(readResourceAsString("common/01-session.json")));
        server.enqueue(
                new MockResponse().setBody(readResourceAsString("common/02-mailboxes.json")));

        final InMemoryCache inMemoryCache = new InMemoryCache();

        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(WELL_KNOWN_PATH))
                        .username(USERNAME)
                        .password(PASSWORD)
                        .accountId(ACCOUNT_ID)
                        .cache(inMemoryCache)
                        .build()) {
            mua.refreshMailboxes().get();

            IdentifiableMailboxWithRole mailbox =
                    MailboxUtil.find(inMemoryCache.getSpecialMailboxes(), Role.IMPORTANT);

            Assertions.assertNotNull(mailbox);

            final Collection<MyIdentifiableEmailWithMailboxes> emails =
                    ImmutableSet.of(new MyIdentifiableEmailWithMailboxes("e0", "mb0"));

            Assertions.assertFalse(mua.removeFromMailbox(emails, mailbox.getId()).get());
        }

        server.shutdown();
    }
}
