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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.api.EndpointNotFoundException;
import rs.ltt.jmap.client.api.HttpJmapApiClient;
import rs.ltt.jmap.client.api.InvalidSessionResourceException;
import rs.ltt.jmap.client.api.UnauthorizedException;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.util.WellKnownUtil;
import rs.ltt.jmap.common.SessionResource;
import rs.ltt.jmap.gson.JmapAdapters;

import java.io.InputStream;
import java.io.InputStreamReader;

public class SessionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionClient.class);

    private final HttpUrl sessionResource;
    private HttpAuthentication httpAuthentication;
    private SessionCache sessionCache;
    private Session currentSession = null;
    private boolean sessionResourceChanged = false;

    public SessionClient(HttpAuthentication authentication) {
        this.sessionResource = null;
        this.httpAuthentication = authentication;
    }

    public SessionClient(HttpAuthentication authentication, HttpUrl sessionResource) {
        this.sessionResource = sessionResource;
        this.httpAuthentication = authentication;
    }

    public Session get() throws Exception {
        synchronized (this) {
            if (!sessionResourceChanged && currentSession != null) {
                return currentSession;
            }

            final String username = httpAuthentication.getUsername();
            final HttpUrl resource;
            if (sessionResource != null) {
                resource = sessionResource;
            } else {
                resource = WellKnownUtil.fromUsername(username);
            }

            final SessionCache cache = sessionCache;
            Session session = !sessionResourceChanged && cache != null ? cache.load(username, resource) : null;

            if (session == null) {
                session = fetchSession(resource);
                sessionResourceChanged = false;
                if (cache != null) {
                    cache.store(username, resource, session);
                }
            }

            currentSession = session;

        }
        return currentSession;
    }

    public void setLatestSessionState(String sessionState) {
        synchronized (this) {
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
    }

    private Session fetchSession(final HttpUrl base) throws Exception {
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(base);
        httpAuthentication.authenticate(requestBuilder);

        final Response response = HttpJmapApiClient.OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute();
        final int code = response.code();
        if (code == 200 || code == 201) {
            final ResponseBody body = response.body();
            if (body == null) {
                throw new InvalidSessionResourceException("Unable to fetch session object. Response body was empty.");
            }
            try (final InputStream inputStream = body.byteStream()) {
                final GsonBuilder builder = new GsonBuilder();
                JmapAdapters.register(builder);
                final Gson gson = builder.create();
                final SessionResource sessionResource;
                try {
                    sessionResource = gson.fromJson(new InputStreamReader(inputStream), SessionResource.class);
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

    public void setSessionCache(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

}
