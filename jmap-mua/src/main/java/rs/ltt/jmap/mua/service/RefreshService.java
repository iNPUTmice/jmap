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

package rs.ltt.jmap.mua.service;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.mua.Status;
import rs.ltt.jmap.mua.cache.ObjectsState;

public class RefreshService extends MuaService {

    public RefreshService(MuaSession muaSession) {
        super(muaSession);
    }

    public ListenableFuture<Status> refresh() {
        return Futures.transformAsync(
                getObjectsState(), this::refresh, MoreExecutors.directExecutor());
    }

    public ListenableFuture<Status> refresh(ObjectsState objectsState) {
        final JmapClient.MultiCall multiCall = jmapClient.newMultiCall();
        List<ListenableFuture<Status>> futuresList = refresh(objectsState, multiCall);
        multiCall.execute();
        return transform(futuresList);
    }

    public List<ListenableFuture<Status>> refresh(
            ObjectsState objectsState, JmapClient.MultiCall multiCall) {
        ImmutableList.Builder<ListenableFuture<Status>> futuresListBuilder =
                new ImmutableList.Builder<>();
        if (objectsState.mailboxState != null) {
            futuresListBuilder.add(
                    getService(MailboxService.class)
                            .updateMailboxes(objectsState.mailboxState, multiCall));
        } else {
            futuresListBuilder.add(getService(MailboxService.class).loadMailboxes(multiCall));
        }

        // update to emails should happen before update to threads
        // when mua queries threads the corresponding emails should already be in the cache

        if (objectsState.emailState != null && objectsState.threadState != null) {
            futuresListBuilder.add(
                    getService(EmailService.class)
                            .updateEmails(objectsState.emailState, multiCall));
            futuresListBuilder.add(
                    getService(ThreadService.class)
                            .updateThreads(objectsState.threadState, multiCall));
        }
        return futuresListBuilder.build();
    }
}
