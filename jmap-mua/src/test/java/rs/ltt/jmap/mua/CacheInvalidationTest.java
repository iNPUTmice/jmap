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
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheInvalidationTest {

    @Test
    public void canNotCalculateChangesQueryRepeat() throws IOException, InterruptedException, ExecutionException {
        final MyMockMailServer myMockMailServer = new MyMockMailServer(2);
        myMockMailServer.setReportCanCalculateQueryChanges(true);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(myMockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(myMockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(myMockMailServer.getAccountId())
                .build()) {
            mua.query(EmailQuery.unfiltered()).get();
            myMockMailServer.bumpVersion();
            final ExecutionException exception = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> mua.query(EmailQuery.unfiltered()).get()
            );
            MatcherAssert.assertThat(exception.getCause(), CoreMatchers.instanceOf(MethodErrorResponseException.class));
        }
        Assertions.assertTrue(myInMemoryCache.queryCacheInvalidationTriggered.get(), "Query Cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.emailCacheInvalidationTriggered.get(), "Email cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.threadCacheInvalidationTriggered.get(), "Thread cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.mailboxCacheInvalidationTriggered.get(), "Mailbox cache has not been invalidated");
        server.shutdown();
    }

    private static class MyInMemoryCache extends InMemoryCache {

        private final AtomicBoolean queryCacheInvalidationTriggered = new AtomicBoolean(false);
        private final AtomicBoolean emailCacheInvalidationTriggered = new AtomicBoolean(false);
        private final AtomicBoolean threadCacheInvalidationTriggered = new AtomicBoolean(false);
        private final AtomicBoolean mailboxCacheInvalidationTriggered = new AtomicBoolean(false);

        @Override
        public void invalidateEmailThreadsAndQueries() {
            super.invalidateEmailThreadsAndQueries();
            this.emailCacheInvalidationTriggered.set(true);
            this.threadCacheInvalidationTriggered.set(true);
            this.queryCacheInvalidationTriggered.set(true);
        }

        @Override
        public void invalidateMailboxes() {
            super.invalidateMailboxes();
            this.mailboxCacheInvalidationTriggered.set(true);
        }

    }

    private static class MyMockMailServer extends MockMailServer {

        public MyMockMailServer(int numThreads) {
            super(numThreads);
        }

        public void bumpVersion() {
            super.incrementState();
        }
    }
}
