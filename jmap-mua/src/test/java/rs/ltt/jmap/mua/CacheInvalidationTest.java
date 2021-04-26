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
import rs.ltt.jmap.common.method.error.AnchorNotFoundMethodErrorResponse;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;
import rs.ltt.jmap.mua.cache.QueryStateWrapper;
import rs.ltt.jmap.mua.util.QueryResultItem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        Assertions.assertEquals(3, myInMemoryCache.queryCacheInvalidationTriggered.get(), "Query Cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.queryCacheInvalidationProper.get(), "Query Cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.emailCacheInvalidationTriggered.get(), "Email cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.threadCacheInvalidationTriggered.get(), "Thread cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.mailboxCacheInvalidationTriggered.get(), "Mailbox cache has not been invalidated");
        server.shutdown();
    }

    @Test
    public void canNotCalculateChangesQueryPaged() throws IOException, InterruptedException, ExecutionException {
        final MyMockMailServer myMockMailServer = new MyMockMailServer(128);
        myMockMailServer.setReportCanCalculateQueryChanges(true);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(myMockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(myMockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(myMockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {
            mua.query(emailQuery).get();
            final List<String> threadIds = myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash());
            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    threadIds
            );
            myMockMailServer.bumpVersion();
            final String lastEmailId = myInMemoryCache.getItems(emailQuery.asHash()).get(4).getEmailId();
            Assertions.assertEquals("M10", lastEmailId);
            final ExecutionException exception = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> mua.query(emailQuery, lastEmailId).get()
            );
            MatcherAssert.assertThat(exception.getCause(), CoreMatchers.instanceOf(MethodErrorResponseException.class));
        }
        Assertions.assertEquals(3, myInMemoryCache.queryCacheInvalidationTriggered.get(), "Query Cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.queryCacheInvalidationProper.get(), "Query Cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.emailCacheInvalidationTriggered.get(), "Email cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.threadCacheInvalidationTriggered.get(), "Thread cache has not been invalidated");
        Assertions.assertTrue(myInMemoryCache.mailboxCacheInvalidationTriggered.get(), "Mailbox cache has not been invalidated");
        server.shutdown();
    }

    //TODO write variant that has queryChanges at the same time and *NOT* invalidates cache
    @Test
    public void invalidationOnAnchorNotFound() throws IOException, InterruptedException, ExecutionException {
        final MyMockMailServer myMockMailServer = new MyMockMailServer(128);
        myMockMailServer.setReportCanCalculateQueryChanges(true);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(myMockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache() {
            @Override
            @Nonnull
            public QueryStateWrapper getQueryState(String query) {
                final QueryStateWrapper queryStateWrapper = super.getQueryState(query);
                return new QueryStateWrapper(
                        queryStateWrapper.queryState,
                        queryStateWrapper.canCalculateChanges,
                        new QueryStateWrapper.UpTo("not-existent", 4),
                        queryStateWrapper.objectsState
                );
            }
        };

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(myMockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(myMockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {
            mua.query(emailQuery).get();
            final List<String> threadIds = myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash());
            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    threadIds
            );
            final ExecutionException exception = Assertions.assertThrows(
                    ExecutionException.class,
                    () -> mua.query(emailQuery, "not-existent").get()
            );
            MatcherAssert.assertThat(exception.getCause(), CoreMatchers.instanceOf(MethodErrorResponseException.class));
            MethodErrorResponseException methodErrorResponseException = (MethodErrorResponseException) exception.getCause();
            MatcherAssert.assertThat(
                    methodErrorResponseException.getMethodErrorResponse(),
                    CoreMatchers.instanceOf(AnchorNotFoundMethodErrorResponse.class)
            );
        }
        Assertions.assertEquals(1, myInMemoryCache.queryCacheInvalidationTriggered.get(), "Query Cache has not been invalidated");
        Assertions.assertFalse(myInMemoryCache.emailCacheInvalidationTriggered.get(), "Email cache should not have been invalidated");
        Assertions.assertFalse(myInMemoryCache.threadCacheInvalidationTriggered.get(), "Thread cache should not have invalidated");
        Assertions.assertFalse(myInMemoryCache.mailboxCacheInvalidationTriggered.get(), "Mailbox cache should not have invalidated");
        server.shutdown();
    }

    private static class MyInMemoryCache extends InMemoryCache {

        private final AtomicInteger queryCacheInvalidationTriggered = new AtomicInteger(0);
        private final AtomicBoolean queryCacheInvalidationProper = new AtomicBoolean(false);
        private final AtomicBoolean emailCacheInvalidationTriggered = new AtomicBoolean(false);
        private final AtomicBoolean threadCacheInvalidationTriggered = new AtomicBoolean(false);
        private final AtomicBoolean mailboxCacheInvalidationTriggered = new AtomicBoolean(false);

        @Override
        public void invalidateEmailThreadsAndQueries() {
            super.invalidateEmailThreadsAndQueries();
            this.emailCacheInvalidationTriggered.set(true);
            this.threadCacheInvalidationTriggered.set(true);
            this.queryCacheInvalidationTriggered.incrementAndGet();
        }

        @Override
        public void invalidateMailboxes() {
            super.invalidateMailboxes();
            this.mailboxCacheInvalidationTriggered.set(true);
        }

        @Override
        public void invalidateQueryResult(final String queryString) {
            super.invalidateQueryResult(queryString);
            this.queryCacheInvalidationTriggered.incrementAndGet()/**/;
            this.queryCacheInvalidationProper.set(true);
        }

        public List<String> getThreadIdsInQuery(final String queryHash) {
            return getItems(queryHash).stream().map(QueryResultItem::getThreadId).collect(Collectors.toList());
        }

        public List<QueryResultItem> getItems(final String queryHash) {
            final InMemoryQueryResult queryResult = queryResults.get(queryHash);
            return queryResult.getItems();
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
