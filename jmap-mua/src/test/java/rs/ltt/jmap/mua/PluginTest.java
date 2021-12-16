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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.plugin.EmailBuildStagePlugin;
import rs.ltt.jmap.mua.plugin.EmailCacheStagePlugin;
import rs.ltt.jmap.mua.service.PluginService;

public class PluginTest {

    @Test
    public void emailCreationPluginTest() throws ExecutionException, InterruptedException {

        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);
        final CountEmailCreationPlugin plugin = new CountEmailCreationPlugin();
        final Identity identity = Identity.builder().name("Stub Identity").build();
        try (final Mua mua =
                Mua.builder()
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .plugin(CountEmailCreationPlugin.class, plugin)
                        .build()) {
            mua.query(EmailQuery.unfiltered(true)).get();
            Assertions.assertEquals(3, plugin.cacheCounter.get());
            final Email email = Email.builder().subject("Stub Email").build();
            mua.draft(email).get();
            mua.query(EmailQuery.unfiltered(true)).get();
            Assertions.assertEquals(1, plugin.buildCounter());

            final Email anotherEmail = Email.builder().subject("Another Stub Email").build();

            Assertions.assertThrows(
                    ExecutionException.class, () -> mua.send(anotherEmail, identity).get());
            Assertions.assertEquals(2, plugin.buildCounter());

            mua.query(EmailQuery.unfiltered(true)).get();

            Assertions.assertEquals(5, plugin.cacheCounter.get());
        }
    }

    private static class CountEmailCreationPlugin extends PluginService.Plugin
            implements EmailBuildStagePlugin, EmailCacheStagePlugin {

        private final AtomicInteger buildCounter = new AtomicInteger();
        private final AtomicInteger cacheCounter = new AtomicInteger();

        @Override
        public ListenableFuture<Email> onBuildEmail(Email email) {
            this.buildCounter.incrementAndGet();
            return Futures.immediateFuture(email);
        }

        public int buildCounter() {
            return this.buildCounter.get();
        }

        @Override
        public void onCacheEmail(Email email) {
            this.cacheCounter.incrementAndGet();
        }
    }
}
