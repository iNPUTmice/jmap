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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Closeable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import rs.ltt.jmap.client.api.JmapApiClient;
import rs.ltt.jmap.client.api.JmapApiClientFactory;
import rs.ltt.jmap.client.api.SessionStateListener;
import rs.ltt.jmap.client.blob.*;
import rs.ltt.jmap.client.event.EventSourcePushService;
import rs.ltt.jmap.client.event.OnStateChangeListener;
import rs.ltt.jmap.client.event.PushService;
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.client.session.SessionCache;
import rs.ltt.jmap.client.session.SessionClient;
import rs.ltt.jmap.client.util.Closeables;
import rs.ltt.jmap.common.entity.Downloadable;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.method.MethodCall;

public class JmapClient implements Closeable {

    private final SessionClient sessionClient;
    private final BinaryDataClient binaryDataClient;
    private final HttpAuthentication authentication;
    private final SessionStateListener sessionStateListener =
            new SessionStateListener() {
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
        this.binaryDataClient = new BinaryDataClient(httpAuthentication);
    }

    public JmapClient(String username, String password, HttpUrl base) {
        this(new BasicAuthHttpAuthentication(username, password), base);
    }

    public JmapClient(HttpAuthentication httpAuthentication, HttpUrl sessionResource) {
        this.authentication = httpAuthentication;
        this.sessionClient = new SessionClient(httpAuthentication, sessionResource);
        this.binaryDataClient = new BinaryDataClient(httpAuthentication);
    }

    public String getUsername() {
        return authentication.getUsername();
    }

    public ListenableFuture<MethodResponses> call(MethodCall methodCall) {
        // TODO check if JmapApiClient has been closed
        // Preconditions.checkState(!isShutdown(), "Unable to call method. JmapClient has been
        // closed already");
        final JmapRequest.Builder jmapRequestBuilder = new JmapRequest.Builder();
        final ListenableFuture<MethodResponses> methodResponsesFuture =
                jmapRequestBuilder.call(methodCall).getMethodResponses();
        this.execute(jmapRequestBuilder.build());
        return methodResponsesFuture;
    }

    private void execute(final JmapRequest request) {
        final ListenableFuture<Session> sessionFuture = getSession();
        request.addDependentFuture(sessionFuture);
        Futures.addCallback(
                sessionFuture,
                new FutureCallback<Session>() {
                    @Override
                    public void onSuccess(@Nullable Session session) {
                        execute(request, session);
                    }

                    @Override
                    public void onFailure(@Nonnull Throwable throwable) {
                        request.setException(throwable);
                    }
                },
                MoreExecutors.directExecutor());
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
            // TODO remember to stop/close invalid clients
            final JmapApiClientFactory factory =
                    new JmapApiClientFactory(authentication, sessionStateListener);
            this.jmapApiClient = factory.getJmapApiClient(session, this.useWebSocket);
            return jmapApiClient;
        }
    }

    public ListenableFuture<Session> getSession() {
        return sessionClient.get();
    }

    public ListenableFuture<PushService> monitorEvents() {
        return monitorEvents(null);
    }

    public ListenableFuture<PushService> monitorEvents(
            @Nullable final OnStateChangeListener onStateChangeListener) {
        return Futures.transform(
                getSession(),
                session -> monitorEvents(session, onStateChangeListener),
                MoreExecutors.directExecutor());
    }

    private PushService monitorEvents(
            final Session session, @Nullable final OnStateChangeListener onStateChangeListener) {
        final JmapApiClient jmapApiClient = getApiClient(session);
        final PushService pushService;
        if (jmapApiClient instanceof PushService) {
            pushService = (PushService) jmapApiClient;
        } else {
            pushService = new EventSourcePushService(session, authentication);
        }
        if (onStateChangeListener != null) {
            pushService.addOnStateChangeListener(onStateChangeListener);
        }
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
                    "WebSocket preference needs to be set before making the first API call");
        }
        this.useWebSocket = useWebSocket;
    }

    public ListenableFuture<Download> download(
            final String accountId, final Downloadable downloadable) {
        return Futures.transformAsync(
                getSession(),
                session -> download(session, accountId, downloadable, 0),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<Download> download(
            final Session session,
            final String accountId,
            final Downloadable downloadable,
            final long rangeStart) {
        final HttpUrl httpUrl = session.getDownloadUrl(accountId, downloadable);
        return this.binaryDataClient.download(httpUrl, rangeStart);
    }

    public ListenableFuture<Download> download(
            final String accountId, final Downloadable downloadable, final long rangeStart) {
        Preconditions.checkArgument(rangeStart >= 0, "rangeStart must not be smaller than 0");
        return Futures.transformAsync(
                getSession(),
                session -> download(session, accountId, downloadable, rangeStart),
                MoreExecutors.directExecutor());
    }

    public ListenableFuture<Upload> upload(
            @Nonnull final String accountId,
            @Nonnull final Uploadable uploadable,
            final Progress progress) {
        Preconditions.checkArgument(accountId != null, "accountId must not be null");
        Preconditions.checkArgument(uploadable != null, "Uploadable must not be null");
        return Futures.transformAsync(
                getSession(),
                session -> upload(session, accountId, uploadable, progress),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<Upload> upload(
            final Session session,
            final String accountId,
            final Uploadable uploadable,
            final Progress progress) {
        final HttpUrl httpUrl = session.getUploadUrl(accountId);
        final CoreCapability coreCapability = session.getCapability(CoreCapability.class);
        final Long maxUploadSize =
                coreCapability == null ? null : coreCapability.getMaxSizeUpload();
        if (maxUploadSize != null && uploadable.getContentLength() > maxUploadSize) {
            return Futures.immediateFailedFuture(
                    new MaxUploadSizeExceededException(
                            uploadable.getContentLength(), maxUploadSize));
        }
        return this.binaryDataClient.upload(httpUrl, uploadable, progress);
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

        private MultiCall() {}

        public synchronized JmapRequest.Call call(MethodCall methodCall) {
            Preconditions.checkState(
                    !executed, "Unable to add MethodCall. MultiCall has already been executed");
            return jmapRequestBuilder.call(methodCall);
        }

        public synchronized void execute() {
            Preconditions.checkState(!executed, "You must not execute the same MultiCall twice");

            // TODO check if jmapApiClient is shutdown or closed or something

            // Preconditions.checkState(!isShutdown(), "Unable to execute MultiCall. JmapClient has
            // been closed already");
            this.executed = true;
            JmapClient.this.execute(jmapRequestBuilder.build());
        }
    }
}
