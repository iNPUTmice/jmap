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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.MethodCall;

public class JmapRequest {

    private final ImmutableMap<Request.Invocation, SettableFuture<MethodResponses>>
            invocationFutureImmutableMap;
    private final Request request;
    private final ArrayList<Future<?>> dependentFutures = new ArrayList<>();

    private JmapRequest(Map<Request.Invocation, SettableFuture<MethodResponses>> map) {
        final Request.Builder requestBuilder = new Request.Builder();
        for (Request.Invocation invocation : map.keySet()) {
            requestBuilder.add(invocation);
        }
        this.request = requestBuilder.build();
        this.invocationFutureImmutableMap = ImmutableMap.copyOf(map);
        Futures.whenAllComplete(this.invocationFutureImmutableMap.values())
                .call(
                        () -> {
                            if (invocationFutureImmutableMap.values().stream()
                                    .allMatch(future -> future.isCancelled())) {
                                dependentFutures.forEach(f -> f.cancel(true));
                            }
                            return null;
                        },
                        MoreExecutors.directExecutor());
    }

    public void addDependentFuture(final Future<?> future) {
        this.dependentFutures.add(future);
    }

    public ImmutableMap<Request.Invocation, SettableFuture<MethodResponses>>
            getInvocationFutureImmutableMap() {
        return invocationFutureImmutableMap;
    }

    public void setException(Throwable throwable) {
        for (SettableFuture<MethodResponses> future : invocationFutureImmutableMap.values()) {
            future.setException(throwable);
        }
    }

    public Request getRequest() {
        return request;
    }

    public static class Builder {

        private final Map<Request.Invocation, SettableFuture<MethodResponses>> map =
                new LinkedHashMap<>();
        private int nextMethodCallId = 0;

        public Call call(final MethodCall methodCall) {
            final Request.Invocation invocation =
                    new Request.Invocation(methodCall, nextMethodCallId());
            final ListenableFuture<MethodResponses> future = add(invocation);
            return new Call(future, invocation);
        }

        private String nextMethodCallId() {
            return Integer.toString(nextMethodCallId++);
        }

        // TODO throw illegal state when adding after build
        private ListenableFuture<MethodResponses> add(final Request.Invocation invocation) {
            final SettableFuture<MethodResponses> future = SettableFuture.create();
            this.map.put(invocation, future);
            return future;
        }

        public JmapRequest build() {
            return new JmapRequest(map);
        }
    }

    public static class Call {
        private final ListenableFuture<MethodResponses> future;
        private final Request.Invocation invocation;

        private Call(ListenableFuture<MethodResponses> future, Request.Invocation invocation) {
            this.future = future;
            this.invocation = invocation;
        }

        public ListenableFuture<MethodResponses> getMethodResponses() {
            return future;
        }

        public Request.Invocation.ResultReference createResultReference(String path) {
            return invocation.createReference(path);
        }
    }
}
