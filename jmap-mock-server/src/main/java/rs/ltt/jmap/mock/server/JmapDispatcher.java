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

package rs.ltt.jmap.mock.server;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import okhttp3.Credentials;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import rs.ltt.jmap.common.*;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.ErrorType;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;

import javax.annotation.Nonnull;


public abstract class JmapDispatcher extends Dispatcher {

    public final EmailAddress account;
    public static final String PASSWORD = "secret";

    private static final Gson GSON;
    public static String WELL_KNOWN_PATH = ".well-known/jmap";

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    private int sessionState = 0;


    public JmapDispatcher(final int accountIndex) {
        this.account = NameGenerator.getEmailAddress((accountIndex + 1) * 2048 + accountIndex);
    }

    public String getUsername() {
        return account.getEmail();
    }

    public String getAccountId() {
        return Hashing.sha256().hashString(account.getEmail(), Charsets.UTF_8).toString();
    }

    @Nonnull
    @Override
    public MockResponse dispatch(final RecordedRequest request) {
        switch (Strings.nullToEmpty(request.getPath())) {
            case "/.well-known/jmap":
                if ("GET".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(301).addHeader("Location: /jmap/");
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            case "/jmap/":
                final String authorization = request.getHeader("Authorization");
                if (!Credentials.basic(getUsername(), PASSWORD).equals(authorization)) {
                    return new MockResponse().setResponseCode(401);
                }
                if ("GET".equals(request.getMethod())) {
                    return session();
                } else if ("POST".equals(request.getMethod())) {
                    return request(request);
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            default:
                return new MockResponse().setResponseCode(404);
        }
    }

    private MockResponse session() {
        final String id = getAccountId();
        final SessionResource sessionResource = SessionResource.builder()
                .apiUrl("/jmap/")
                .state(getSessionState())
                .account(id, Account.builder()
                        .accountCapabilities(ImmutableMap.of(
                                MailAccountCapability.class,
                                MailAccountCapability.builder().build()
                        ))
                        .name(account.getEmail())
                        .build())
                .capabilities(ImmutableMap.of(CoreCapability.class, CoreCapability.builder().maxObjectsInGet(4096L).build()))
                .primaryAccounts(ImmutableMap.of(MailAccountCapability.class, id))
                .build();

        return new MockResponse().setBody(GSON.toJson(sessionResource));
    }

    protected String getSessionState() {
        return String.valueOf(this.sessionState);
    }

    private MockResponse request(final RecordedRequest request) {
        final String contentType = Strings.nullToEmpty(request.getHeader("Content-Type"));
        if (!"application/json".equals(Iterables.getFirst(Splitter.on(';').split(contentType), null))) {
            return new MockResponse()
                    .setResponseCode(400)
                    .setBody(GSON.toJson(new ErrorResponse(ErrorType.NOT_JSON, 400, "Unsupported content type")));
        }
        final Request jmapRequest;
        try {
            jmapRequest = GSON.fromJson(request.getBody().readUtf8(), Request.class);
        } catch (final JsonParseException e) {
            return new MockResponse()
                    .setResponseCode(400)
                    .setBody(GSON.toJson(new ErrorResponse(ErrorType.NOT_JSON, 400, e.getMessage())));
        }
        final GenericResponse response = dispatch(jmapRequest);
        if (response instanceof ErrorResponse) {
            return new MockResponse().setResponseCode(400).setBody(GSON.toJson(response));
        }
        return new MockResponse().setResponseCode(200).setBody(GSON.toJson(response));
    }

    protected GenericResponse dispatch(final Request request) {
        final Request.Invocation[] methodCalls = request.getMethodCalls();
        final String[] using = request.getUsing();
        if (using == null || methodCalls == null) {
            return new ErrorResponse(ErrorType.NOT_REQUEST, 400);
        }
        final ArrayListMultimap<String, Response.Invocation> response = ArrayListMultimap.create();
        for (final Request.Invocation invocation : methodCalls) {
            final String id = invocation.getId();
            final MethodCall methodCall = invocation.getMethodCall();
            for (MethodResponse methodResponse : dispatch(methodCall, ImmutableListMultimap.copyOf(response))) {
                response.put(id, new Response.Invocation(methodResponse, id));
            }
        }
        return new Response(
                response.values().toArray(new Response.Invocation[0]),
                getSessionState()
        );
    }

    protected abstract MethodResponse[] dispatch(
            final MethodCall methodCall,
            final ListMultimap<String, Response.Invocation> previousResponses
    );

    protected void incrementSessionState() {
        this.sessionState++;
    }
}
