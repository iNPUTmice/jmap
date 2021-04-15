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

package rs.ltt.jmap.client.session;

import com.google.common.util.concurrent.*;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.util.WellKnownUtil;
import rs.ltt.jmap.common.SessionResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static rs.ltt.jmap.client.Services.GSON;
import static rs.ltt.jmap.client.Services.OK_HTTP_CLIENT_LOGGING;

public class SessionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionClient.class);

    private final HttpUrl sessionResource;
    private final HttpAuthentication httpAuthentication;
    private SessionCache sessionCache;
    private Session currentSession = null;
    private ListenableFuture<Session> currentSessionFuture = Futures.immediateCancelledFuture();
    private boolean sessionResourceChanged = false;

    public SessionClient(final HttpAuthentication authentication) {
        this.sessionResource = null;
        this.httpAuthentication = authentication;
    }

    public SessionClient(final HttpAuthentication authentication, final HttpUrl sessionResource) {
        this.sessionResource = sessionResource;
        this.httpAuthentication = authentication;
    }

    public synchronized ListenableFuture<Session> get() {
        if (!sessionResourceChanged && currentSession != null) {
            return Futures.immediateFuture(currentSession);
        }
        final String username = httpAuthentication.getUsername();
        final HttpUrl resource;
        try {
            resource = getSessionResource();
        } catch (final WellKnownUtil.MalformedUsernameException e) {
            return Futures.immediateFailedFuture(e);
        }
        if (!currentSessionFuture.isDone()) {
            return currentSessionFuture;
        }
        this.currentSessionFuture = fetchSession(username, resource);
        return this.currentSessionFuture;
    }

    private HttpUrl getSessionResource() throws WellKnownUtil.MalformedUsernameException {
        final String username = httpAuthentication.getUsername();
        if (sessionResource != null) {
            return sessionResource;
        } else {
            return WellKnownUtil.fromUsername(username);
        }
    }

    private ListenableFuture<Session> fetchSession(final String username, final HttpUrl sessionResource) {
        final SessionCache cache = sessionResourceChanged ? null : sessionCache;
        final ListenableFuture<Session> cachedSessionFuture;
        if (cache == null) {
            cachedSessionFuture = Futures.immediateFuture(null);
        } else {
            cachedSessionFuture = cache.load(username, sessionResource);
        }
        return Futures.transformAsync(cachedSessionFuture, session -> {
            if (session != null) {
                return Futures.immediateFuture(session);
            }
            return fetchSessionHttp(username, sessionResource);
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Session> fetchSessionHttp(final String username, final HttpUrl base) throws Exception {
        final SettableFuture<Session> settableFuture = SettableFuture.create();
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(base);
        httpAuthentication.authenticate(requestBuilder);

        final Call call = OK_HTTP_CLIENT_LOGGING.newCall(requestBuilder.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                settableFuture.setException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    settableFuture.set(processResponse(base, response));
                } catch (final Exception e) {
                    settableFuture.setException(e);
                }
            }
        });
        registerSuccessCallback(settableFuture, username, base);
        return settableFuture;
    }

    private static Session processResponse(final HttpUrl base, final Response response) throws Exception {
        final int code = response.code();
        if (code == 200 || code == 201) {
            final ResponseBody body = response.body();
            if (body == null) {
                throw new InvalidSessionResourceException("Unable to fetch session object. Response body was empty.");
            }
            try (final InputStream inputStream = body.byteStream()) {
                final SessionResource sessionResource;
                try {
                    sessionResource = GSON.fromJson(new InputStreamReader(inputStream), SessionResource.class);
                } catch (JsonIOException | JsonSyntaxException e) {
                    throw new InvalidSessionResourceException(e);
                }
                final HttpUrl currentBaseUrl = response.request().url();
                if (!base.equals(currentBaseUrl)) {
                    LOGGER.info("Processed new base URL {}", currentBaseUrl.url());
                }
                return new Session(currentBaseUrl, sessionResource);
            }
        } else if (code == 401) {
            throw new UnauthorizedException(String.format("Session object(%s) was unauthorized", base.toString()));
        } else {
            throw new EndpointNotFoundException(String.format("Unable to fetch session object(%s)", base.toString()));
        }
    }

    private void registerSuccessCallback(final ListenableFuture<Session> future, final String username, final HttpUrl resource) {
        Futures.addCallback(future, new FutureCallback<Session>() {
            @Override
            public void onSuccess(@Nullable Session session) {
                if (session != null) {
                    setSession(username, resource, session);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {

            }
        }, MoreExecutors.directExecutor());
    }

    private synchronized void setSession(String username, HttpUrl resource, final Session session) {
        this.sessionResourceChanged = false;
        this.currentSession = session;
        final SessionCache cache = sessionCache;
        if (cache != null) {
            LOGGER.debug("caching to {}", cache.getClass().getSimpleName());
            cache.store(username, resource, session);
        }
    }

    public synchronized void setLatestSessionState(String sessionState) {
        if (sessionResourceChanged) {
            return;
        }

        final Session existingSession = this.currentSession;
        if (existingSession == null) {
            sessionResourceChanged = true;
            return;
        }

        final String oldState = existingSession.getState();
        if (oldState == null || !oldState.equals(sessionState)) {
            sessionResourceChanged = true;
        }
    }

    public void setSessionCache(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

}
