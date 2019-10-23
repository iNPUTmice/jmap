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


import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import rs.ltt.jmap.client.api.HttpJmapApiClient;
import rs.ltt.jmap.client.api.JmapApiClient;
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.client.session.SessionCache;
import rs.ltt.jmap.client.session.SessionClient;
import rs.ltt.jmap.common.method.MethodCall;

import java.io.Closeable;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class JmapClient implements Closeable {

    private final SessionClient sessionClient;
    private final HttpAuthentication authentication;

    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    public JmapClient(HttpAuthentication httpAuthentication) {
        this.authentication = httpAuthentication;
        this.sessionClient = new SessionClient(httpAuthentication);
    }

    public JmapClient(HttpAuthentication httpAuthentication, URL sessionResource) {
        this.authentication = httpAuthentication;
        this.sessionClient = new SessionClient(httpAuthentication, sessionResource);
    }


    public JmapClient(String username, String password) {
        this(new BasicAuthHttpAuthentication(username, password));
    }

    public JmapClient(String username, String password, URL base) {
        this(new BasicAuthHttpAuthentication(username, password), base);
    }

    public ListenableFuture<URL> getBaseUrl() {
        Preconditions.checkState(!isShutdown(), "Unable to get baseUrl. JmapClient has been closed already");
        return Futures.transform(loadSession(), new Function<Session, URL>() {
            @NullableDecl
            @Override
            public URL apply(@NullableDecl Session session) {
                return session != null ? session.getBase() : null;
            }
        }, MoreExecutors.directExecutor());
    }

    public String getUsername() {
        return authentication.getUsername();
    }

    private ListenableFuture<Session> loadSession() {
        return executorService.submit(new Callable<Session>() {
            @Override
            public Session call() throws Exception {
                return sessionClient.get();
            }
        });
    }

    public ListenableFuture<MethodResponses> call(MethodCall methodCall) {
        Preconditions.checkState(!isShutdown(), "Unable to call method. JmapClient has been closed already");
        final JmapRequest.Builder jmapRequestBuilder = new JmapRequest.Builder();
        final ListenableFuture<MethodResponses> methodResponsesFuture = jmapRequestBuilder.call(methodCall).getMethodResponses();
        this.execute(jmapRequestBuilder.build());
        return methodResponsesFuture;
    }

    private void execute(final JmapRequest request) {
        Futures.addCallback(loadSession(), new FutureCallback<Session>() {
            @Override
            public void onSuccess(@NullableDecl Session session) {
                if (session == null) {
                    return;
                }
                JmapApiClient apiClient = new HttpJmapApiClient(session.getApiUrl(), authentication);
                apiClient.execute(request);
            }

            @Override
            public void onFailure(@NonNullDecl Throwable throwable) {
                request.setException(throwable);
            }
        }, executorService);
    }

    public MultiCall newMultiCall() {
        return new MultiCall();
    }

    public void setSessionCache(SessionCache sessionCache) {
        this.sessionClient.setSessionCache(sessionCache);
    }

    private boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    public class MultiCall {

        private boolean executed = false;

        private final JmapRequest.Builder jmapRequestBuilder = new JmapRequest.Builder();

        private MultiCall() {

        }

        public synchronized JmapRequest.Call call(MethodCall methodCall) {
            Preconditions.checkState(!executed, "Unable to add MethodCall. MultiCall has already been executed");
            return jmapRequestBuilder.call(methodCall);
        }

        public synchronized void execute() {
            Preconditions.checkState(!executed,"You must not execute the same MultiCall twice");
            Preconditions.checkState(!isShutdown(), "Unable to execute MultiCall. JmapClient has been closed already");
            executed = true;
            JmapClient.this.execute(jmapRequestBuilder.build());
        }

    }

}
