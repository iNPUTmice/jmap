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

package rs.ltt.jmap.mua.service;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.mua.Status;
import rs.ltt.jmap.mua.cache.Update;
import rs.ltt.jmap.mua.util.UpdateUtil;

public class ThreadService extends AbstractMuaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadService.class);

    public ThreadService(MuaSession muaSession) {
        super(muaSession);
    }

    protected ListenableFuture<Status> updateThreads(
            final String state, final JmapClient.MultiCall multiCall) {
        Preconditions.checkNotNull(state, "state can not be null when updating threads");
        LOGGER.info("Refreshing threads since state {}", state);
        final UpdateUtil.MethodResponsesFuture methodResponsesFuture =
                UpdateUtil.threads(multiCall, accountId, state);
        registerCacheInvalidationCallback(methodResponsesFuture, this::invalidateCache);
        return methodResponsesFuture.addCallback(
                () -> {
                    final ChangesThreadMethodResponse changesResponse =
                            methodResponsesFuture.changes(ChangesThreadMethodResponse.class);
                    final GetThreadMethodResponse createdResponse =
                            methodResponsesFuture.created(GetThreadMethodResponse.class);
                    final GetThreadMethodResponse updatedResponse =
                            methodResponsesFuture.updated(GetThreadMethodResponse.class);
                    final Update<Thread> update =
                            Update.of(changesResponse, createdResponse, updatedResponse);
                    if (update.hasChanges()) {
                        cache.updateThreads(update);
                    }
                    return Futures.immediateFuture(Status.of(update));
                },
                ioExecutorService);
    }

    private void invalidateCache() {
        LOGGER.info("Invalidate threads cache after cannotCalculateChanges response");
        cache.invalidateEmailThreadsAndQueries();
    }
}
