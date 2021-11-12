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

import static rs.ltt.jmap.client.Services.GSON;
import static rs.ltt.jmap.client.Services.OK_HTTP_CLIENT_LOGGING;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStreamReader;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.util.SettableCallFuture;
import rs.ltt.jmap.client.util.WellKnownUtil;
import rs.ltt.jmap.common.SessionResource;

import java.io.BufferedReader;
import java.util.stream.Collectors;

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

    private ListenableFuture<Session> fetchSession(
            final String username, final HttpUrl sessionResource) {
        final SessionCache cache = sessionResourceChanged ? null : sessionCache;
        final ListenableFuture<Session> cachedSessionFuture;
        if (cache == null) {
            cachedSessionFuture = Futures.immediateFuture(null);
        } else {
            cachedSessionFuture = cache.load(username, sessionResource);
        }
        return Futures.transformAsync(
                cachedSessionFuture,
                session -> {
                    if (session != null) {
                        synchronized (this) {
                            this.currentSession = session;
                        }
                        return Futures.immediateFuture(session);
                    }
                    return fetchSession(sessionResource);
                },
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<Session> fetchSession(final HttpUrl sessionResource) {
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(sessionResource);
        httpAuthentication.authenticate(requestBuilder);
        final Call call = OK_HTTP_CLIENT_LOGGING.newCall(requestBuilder.build());
        final SettableCallFuture<Session> settableFuture = SettableCallFuture.create(call);
        call.enqueue(
                new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        settableFuture.setException(e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try {
                            settableFuture.set(processResponse(sessionResource, response));
                        } catch (final Exception e) {
                            settableFuture.setException(e);
                        }
                    }
                });
        return settableFuture;
    }

    private Session processResponse(final HttpUrl base, final Response response) throws Exception {
        final int code = response.code();
        if (code == 200 || code == 201) {
            final ResponseBody body = response.body();
            if (body == null) {
                throw new InvalidSessionResourceException(
                        "Unable to fetch session object. Response body was empty.");
            }

            String json_str = "Unreadable InputStream";

            try (final InputStreamReader reader = new InputStreamReader(body.byteStream())) {
                final SessionResource sessionResource;
                try {
                    json_str = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
                    sessionResource = GSON.fromJson(json_str, SessionResource.class);
                } catch (JsonIOException | JsonSyntaxException e) {
                    if (json_str.length() > 1000)
                        json_str = json_str.substring(0,1000) + "...";

                    LOGGER.warn("Invalid JSON: " + json_str);
                    throw new InvalidSessionResourceException(e);
                }
                validateSessionResource(sessionResource);
                final HttpUrl currentBaseUrl = response.request().url();
                if (!base.equals(currentBaseUrl)) {
                    LOGGER.info("Processed new base URL {}", currentBaseUrl.url());
                }
                final Session session = new Session(currentBaseUrl, sessionResource);
                setSession(base, session);
                return session;
            }
        } else if (code == 401) {
            throw new UnauthorizedException(
                    String.format("Session object(%s) was unauthorized", base.toString()));
        } else {
            throw new EndpointNotFoundException(
                    String.format("Unable to fetch session object(%s)", base.toString()));
        }
    }

    private void validateSessionResource(final SessionResource sessionResource)
            throws InvalidSessionResourceException {
        if (sessionResource.getApiUrl() == null) {
            throw new InvalidSessionResourceException("Missing API URL");
        }
    }

    private synchronized void setSession(final HttpUrl resource, final Session session) {
        this.sessionResourceChanged = false;
        this.currentSession = session;
        final SessionCache cache = sessionCache;
        if (cache != null) {
            final String username = httpAuthentication.getUsername();
            LOGGER.debug("caching to {}", cache.getClass().getSimpleName());
            cache.store(username, resource, session);
        }
    }

    public synchronized void setLatestSessionState(final String sessionState) {
        if (sessionResourceChanged) {
            return;
        }

        final Session existingSession = this.currentSession;
        if (existingSession == null) {
            LOGGER.warn("Flag existing session as changed after session was null");
            sessionResourceChanged = true;
            return;
        }

        final String oldState = existingSession.getState();
        if (oldState == null || !oldState.equals(sessionState)) {
            LOGGER.warn("Flag existing session as changed. was {} is {}", oldState, sessionState);
            sessionResourceChanged = true;
        }
    }

    public void setSessionCache(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }
}
