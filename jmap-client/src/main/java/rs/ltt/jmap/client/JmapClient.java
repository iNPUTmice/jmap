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


import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import okhttp3.HttpUrl;
import rs.ltt.jmap.client.api.JmapApiClient;
import rs.ltt.jmap.client.api.JmapApiClientFactory;
import rs.ltt.jmap.client.api.SessionStateListener;
import rs.ltt.jmap.client.event.EventSourcePushService;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.client.session.SessionCache;
import rs.ltt.jmap.client.session.SessionClient;
import rs.ltt.jmap.client.util.Closeables;
import rs.ltt.jmap.common.method.MethodCall;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.concurrent.Executors;

public class JmapClient implements Closeable {

    private final SessionClient sessionClient;
    private final HttpAuthentication authentication;
    private final SessionStateListener sessionStateListener = new SessionStateListener() {
        @Override
        public void onSessionStateRetrieved(String sessionState) {
            sessionClient.setLatestSessionState(sessionState);
        }
    };
    private JmapApiClient jmapApiClient;
    private boolean useWebSocket = false;

    public JmapClient(String username, String password) {
        this(new BasicAuthHttpAuthentication(username, password));
    }

    public JmapClient(HttpAuthentication httpAuthentication) {
        this.authentication = httpAuthentication;
        this.sessionClient = new SessionClient(httpAuthentication);
    }


    public JmapClient(String username, String password, HttpUrl base) {
        this(new BasicAuthHttpAuthentication(username, password), base);
    }

    public JmapClient(HttpAuthentication httpAuthentication, HttpUrl sessionResource) {
        this.authentication = httpAuthentication;
        this.sessionClient = new SessionClient(httpAuthentication, sessionResource);
    }

    public String getUsername() {
        return authentication.getUsername();
    }

    public ListenableFuture<MethodResponses> call(MethodCall methodCall) {
        //TODO check if JmapApiClient has been closed
        //Preconditions.checkState(!isShutdown(), "Unable to call method. JmapClient has been closed already");
        final JmapRequest.Builder jmapRequestBuilder = new JmapRequest.Builder();
        final ListenableFuture<MethodResponses> methodResponsesFuture = jmapRequestBuilder.call(methodCall).getMethodResponses();
        this.execute(jmapRequestBuilder.build());
        return methodResponsesFuture;
    }

    private void execute(final JmapRequest request) {
        Futures.addCallback(getSession(), new FutureCallback<Session>() {
            @Override
            public void onSuccess(@Nullable Session session) {
                execute(request, session);
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
                request.setException(throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    private void execute(final JmapRequest request, final Session session) {
        try {
            Preconditions.checkState(session != null, "Session was null");
            final JmapApiClient apiClient = getApiClient(session);
            apiClient.execute(request);
        } catch (final Throwable throwable) {
            request.setException(throwable);
        }
    }

    private JmapApiClient getApiClient(final Session session) {
        final JmapApiClient current = this.jmapApiClient;
        if (current != null && current.isValidFor(session)) {
            return current;
        }
        synchronized (this) {
            if (this.jmapApiClient != null && this.jmapApiClient.isValidFor(session)) {
                return this.jmapApiClient;
            }
            //TODO remember to stop/close invalid clients
            final JmapApiClientFactory factory = new JmapApiClientFactory(
                    authentication,
                    sessionStateListener
            );
            this.jmapApiClient = factory.getJmapApiClient(session, this.useWebSocket);
            return jmapApiClient;
        }
    }

    public ListenableFuture<Session> getSession() {
        return sessionClient.get();
    }

    public ListenableFuture<PushService> monitorEvents(final OnStateChangeListener onStateChangeListener) {
        return Futures.transform(
                getSession(),
                session -> monitorEvents(session, onStateChangeListener),
                MoreExecutors.directExecutor()
        );
    }

    private PushService monitorEvents(final Session session, final OnStateChangeListener onStateChangeListener) {
        final JmapApiClient jmapApiClient = getApiClient(session);
        final PushService pushService;
        if (jmapApiClient instanceof PushService) {
            pushService = (PushService) jmapApiClient;
        } else {
            pushService = new EventSourcePushService(
                    session,
                    authentication
            );
        }
        pushService.addOnStateChangeListener(onStateChangeListener);
        return pushService;
    }

    public MultiCall newMultiCall() {
        return new MultiCall();
    }

    public void setSessionCache(SessionCache sessionCache) {
        this.sessionClient.setSessionCache(sessionCache);
    }

    public void setUseWebSocket(final boolean useWebSocket) {
        synchronized (this) {
            Preconditions.checkState(
                    this.jmapApiClient == null,
                    "WebSocket preference needs to be set before making the first API call"
            );
        }
        this.useWebSocket = useWebSocket;
    }

    @Override
    public void close() {
        final JmapApiClient apiClient = this.jmapApiClient;
        if (apiClient instanceof Closeable) {
            Closeables.closeQuietly((Closeable) apiClient);
        }
    }

    public class MultiCall {

        private final JmapRequest.Builder jmapRequestBuilder = new JmapRequest.Builder();
        private boolean executed = false;

        private MultiCall() {

        }

        public synchronized JmapRequest.Call call(MethodCall methodCall) {
            Preconditions.checkState(!executed, "Unable to add MethodCall. MultiCall has already been executed");
            return jmapRequestBuilder.call(methodCall);
        }

        public synchronized void execute() {
            Preconditions.checkState(!executed, "You must not execute the same MultiCall twice");

            //TODO check if jmapApiClient is shutdown or closed or something

            //Preconditions.checkState(!isShutdown(), "Unable to execute MultiCall. JmapClient has been closed already");
            this.executed = true;
            JmapClient.this.execute(jmapRequestBuilder.build());
        }

    }

}
