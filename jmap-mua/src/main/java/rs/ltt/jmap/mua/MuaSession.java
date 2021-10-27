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

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Closeable;
import java.util.concurrent.Executors;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.mua.cache.Cache;
import rs.ltt.jmap.mua.service.*;

public class MuaSession implements Closeable {

    protected final JmapClient jmapClient;
    private final Cache cache;
    private final String accountId;
    private final ListeningExecutorService ioExecutorService =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final ImmutableClassToInstanceMap<MuaService> services;
    private Long queryPageSize = null;

    public MuaSession(final JmapClient jmapClient, final Cache cache, final String accountId) {
        this.jmapClient = jmapClient;
        this.cache = cache;
        this.accountId = accountId;
        this.services =
                ImmutableClassToInstanceMap.<MuaService>builder()
                        .put(EmailService.class, new EmailService(this))
                        .put(IdentityService.class, new IdentityService(this))
                        .put(MailboxService.class, new MailboxService(this))
                        .put(QueryService.class, new QueryService(this))
                        .put(RefreshService.class, new RefreshService(this))
                        .put(ThreadService.class, new ThreadService(this))
                        .build();
    }

    public <T extends MuaService> T getService(Class<T> clazz) {
        return services.getInstance(clazz);
    }

    @Override
    public void close() {
        ioExecutorService.shutdown();
        jmapClient.close();
    }

    public JmapClient getJmapClient() {
        return jmapClient;
    }

    public Cache getCache() {
        return cache;
    }

    public String getAccountId() {
        return accountId;
    }

    public ListeningExecutorService getIoExecutorService() {
        return ioExecutorService;
    }

    public Long getQueryPageSize() {
        return queryPageSize;
    }

    public void setQueryPageSize(Long queryPageSize) {
        this.queryPageSize = queryPageSize;
    }
}
