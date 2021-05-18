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

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;
import rs.ltt.jmap.mua.util.QueryResult;
import rs.ltt.jmap.mua.util.QueryResultItem;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class QueryTest {

    @Test
    public void queryRefresh() throws IOException, InterruptedException, ExecutionException {
        final MockMailServer mockMailServer = new MockMailServer(128);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(mockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(mockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {

            mua.query(emailQuery).get();

            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash())
            );

            mockMailServer.generateEmailOnTop();

            mua.query(emailQuery).get();

            final List<String> threadsAfterRefresh = myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash());

            Assertions.assertEquals(5, threadsAfterRefresh.size());

            Assertions.assertEquals("T0", threadsAfterRefresh.get(1));
            Assertions.assertEquals("T3", threadsAfterRefresh.get(4));

        }

        Assertions.assertFalse(myInMemoryCache.hadTotal.get(),"QueryResult had total count");

        server.shutdown();
    }

    @Test
    public void queryCalculateTotal() throws IOException, InterruptedException, ExecutionException {
        final MockMailServer mockMailServer = new MockMailServer(128);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(mockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(mockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mockMailServer.getAccountId())
                .build()) {

            mua.query(emailQuery, true).get();
        }

        Assertions.assertTrue(myInMemoryCache.hadTotal.get(),"QueryResult did not have total");

        server.shutdown();
    }

    @Test
    public void queryPagination() throws IOException, InterruptedException, ExecutionException {
        final MockMailServer mockMailServer = new MockMailServer(128);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(mockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(mockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {

            mua.query(emailQuery).get();

            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash())
            );

            final String lastEmailId = myInMemoryCache.getItems(emailQuery.asHash()).get(4).getEmailId();

            Assertions.assertEquals("M10", lastEmailId);


            mua.query(emailQuery, lastEmailId).get();

            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9"),
                    myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash())
            );
        }

        Assertions.assertFalse(myInMemoryCache.hadTotal.get(),"QueryResult had total count");

        server.shutdown();
    }

    @Test
    public void queryRefreshMoreThanPageSize() throws IOException, InterruptedException, ExecutionException {
        final MockMailServer mockMailServer = new MockMailServer(128);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(mockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(mockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {

            mua.query(emailQuery).get();

            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash())
            );

            final String lastEmailId = myInMemoryCache.getItems(emailQuery.asHash()).get(4).getEmailId();

            Assertions.assertEquals("M10", lastEmailId);


            mua.query(emailQuery, lastEmailId).get();

            mockMailServer.generateEmailOnTop();

            mua.query(emailQuery).get();

            final List<String> threadIds = myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash());

            Assertions.assertEquals(10, threadIds.size());

            Assertions.assertEquals("T8", threadIds.get(9));
        }

        server.shutdown();
    }

    @Test
    public void queryRefreshMoreThanPageSizeAndGetLimit() throws IOException, InterruptedException, ExecutionException {
        final MockMailServer mockMailServer = new MockMailServer(128);
        mockMailServer.setMaxObjectsInGet(8);
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(mockMailServer);

        final MyInMemoryCache myInMemoryCache = new MyInMemoryCache();

        final EmailQuery emailQuery = EmailQuery.unfiltered(true);

        try (final Mua mua = Mua.builder()
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .cache(myInMemoryCache)
                .username(mockMailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mockMailServer.getAccountId())
                .queryPageSize(5)
                .build()) {

            mua.query(emailQuery).get();

            Assertions.assertEquals(
                    Arrays.asList("T0", "T1", "T2", "T3", "T4"),
                    myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash())
            );

            final String lastEmailId = myInMemoryCache.getItems(emailQuery.asHash()).get(4).getEmailId();

            Assertions.assertEquals("M10", lastEmailId);


            mua.query(emailQuery, lastEmailId).get();

            mockMailServer.generateEmailOnTop();

            mua.query(emailQuery).get();

            final List<String> threadIds = myInMemoryCache.getThreadIdsInQuery(emailQuery.asHash());

            Assertions.assertEquals(8, threadIds.size());

            Assertions.assertEquals("T6", threadIds.get(7));
        }

        server.shutdown();
    }

    private static class MyInMemoryCache extends InMemoryCache {

        private final AtomicBoolean hadTotal = new AtomicBoolean(false);

        public List<String> getThreadIdsInQuery(final String queryHash) {
            return getItems(queryHash).stream().map(QueryResultItem::getThreadId).collect(Collectors.toList());
        }

        public List<QueryResultItem> getItems(final String queryHash) {
            final InMemoryQueryResult queryResult = queryResults.get(queryHash);
            return queryResult.getItems();
        }

        @Override
        public void setQueryResult(String query, QueryResult queryResult) {
            this.hadTotal.compareAndSet(false, queryResult.total != null);
            super.setQueryResult(query, queryResult);
        }

    }
}
