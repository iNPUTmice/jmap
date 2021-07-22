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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rs.ltt.jmap.common.*;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.ErrorType;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.WebSocketCapability;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.websocket.*;
import rs.ltt.jmap.gson.JmapAdapters;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


public abstract class JmapDispatcher extends Dispatcher {

    public static final String PASSWORD = "secret";
    protected static final Gson GSON;
    public static final String WELL_KNOWN_PATH = "/.well-known/jmap";
    private static final String API_PATH = "/jmap/";
    private static final String UPLOAD_PATH = "/upload/";
    private static final String WEB_SOCKET_PATH = "/jmap/ws";



    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    public final EmailAddress account;
    private int sessionState = 0;

    protected final List<WebSocket> pushEnabledWebSockets = new ArrayList<>();

    private FailureTrigger failureTrigger = FailureTrigger.NONE;
    private boolean advertiseWebSocket = true;

    private long maxObjectsInGet = 4096;


    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosing(webSocket, code, reason);
            System.out.println("MockServer received code "+code);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable okhttp3.Response response) {
            super.onFailure(webSocket, t, response);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);
            if (failureTrigger == FailureTrigger.CLOSE) {
                webSocket.close(1000,null);
                return;
            }
            if (failureTrigger == FailureTrigger.IGNORE) {
                return;
            }
            if (failureTrigger == FailureTrigger.INVALID) {
                webSocket.send("[]");
                return;
            }
            final WebSocketMessage webSocketMessage = GSON.fromJson(text, WebSocketMessage.class);
            if (webSocketMessage instanceof RequestWebSocketMessage) {
                final AbstractApiWebSocketMessage response = dispatch(((RequestWebSocketMessage) webSocketMessage));
                webSocket.send(GSON.toJson(response));
                return;
            }
            if (webSocketMessage instanceof PushEnableWebSocketMessage) {
                if (pushEnabledWebSockets.contains(webSocket)) {
                    System.out.println("skip adding socket");
                    return;
                }
                pushEnabledWebSockets.add(webSocket);
                System.out.println("added socket for a total of "+pushEnabledWebSockets.size());
                return;
            }
            if (webSocketMessage instanceof PushDisableWebSocketMessage) {
                pushEnabledWebSockets.remove(webSocket);
                return;
            }
            //TODO support Push enable / push disable
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull okhttp3.Response response) {
            super.onOpen(webSocket, response);
        }
    };


    public JmapDispatcher(final int accountIndex) {
        this.account = NameGenerator.getEmailAddress((accountIndex + 1) * 2048 + accountIndex);
    }

    public void setAdvertiseWebSocket(final boolean advertiseWebSocket) {
        this.advertiseWebSocket = advertiseWebSocket;
    }

    public void setMaxObjectsInGet(final long maxObjectsInGet) {
        this.maxObjectsInGet = maxObjectsInGet;
    }

    public void setFailureTrigger(final FailureTrigger failureTrigger) {
        this.failureTrigger = failureTrigger;
    }

    public boolean hasPushEnabledWebSockets() {
        return !this.pushEnabledWebSockets.isEmpty();
    }

    @Nonnull
    @Override
    public MockResponse dispatch(final RecordedRequest request) {
        switch (Strings.nullToEmpty(request.getPath())) {
            case WELL_KNOWN_PATH:
                return dispatchWellKnown(request);
            case API_PATH:
                return dispatchJmap(request);
            case WEB_SOCKET_PATH:
                return dispatchJmapWebSocket(request);
            default:
                return new MockResponse().setResponseCode(404);
        }
    }

    private MockResponse dispatchWellKnown(final RecordedRequest request) {
        if ("GET".equals(request.getMethod())) {
            return new MockResponse().setResponseCode(301).addHeader("Location: /jmap/");
        } else {
            return new MockResponse().setResponseCode(404);
        }
    }

    private MockResponse dispatchJmap(final RecordedRequest request) {
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
    }

    private MockResponse dispatchJmapWebSocket(final RecordedRequest request) {
        final String authorization = request.getHeader("Authorization");
        if (!Credentials.basic(getUsername(), PASSWORD).equals(authorization)) {
            return new MockResponse().setResponseCode(401);
        }
        if ("GET".equals(request.getMethod())) {
            //TODO check that proper protocol is set
            return new MockResponse().withWebSocketUpgrade(webSocketListener);
        } else {
            return new MockResponse().setResponseCode(404);
        }
    }


    public String getUsername() {
        return account.getEmail();
    }

    private MockResponse session() {
        ImmutableMap.Builder<Class<? extends Capability>, Capability> capabilityBuilder = ImmutableMap.builder();
        capabilityBuilder.put(CoreCapability.class, CoreCapability.builder()
                .maxSizeUpload(100 * 1024 * 1024L) //100MiB
                .maxObjectsInGet(maxObjectsInGet)
                .build());
        if (this.advertiseWebSocket) {
            capabilityBuilder.put(WebSocketCapability.class, WebSocketCapability.builder()
                    .url(WEB_SOCKET_PATH)
                    .supportsPush(true)
                    .build());
        }
        final String id = getAccountId();
        final SessionResource sessionResource = SessionResource.builder()
                .apiUrl(API_PATH)
                .uploadUrl(UPLOAD_PATH)
                .state(getSessionState())
                .account(id, Account.builder()
                        .accountCapabilities(ImmutableMap.of(
                                MailAccountCapability.class,
                                MailAccountCapability.builder()
                                        .maxSizeAttachmentsPerEmail(50 * 1024 * 1024L) //50MiB
                                        .build()
                        ))
                        .name(account.getEmail())
                        .build())
                .capabilities(capabilityBuilder.build())
                .primaryAccounts(ImmutableMap.of(MailAccountCapability.class, id))
                .build();

        return new MockResponse().setBody(GSON.toJson(sessionResource));
    }

    public String getAccountId() {
        return Hashing.sha256().hashString(account.getEmail(), Charsets.UTF_8).toString();
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

    private AbstractApiWebSocketMessage dispatch(RequestWebSocketMessage webSocketMessage) {
        final String id = webSocketMessage.getRequestId();
        final GenericResponse response = dispatch(webSocketMessage.getRequest());
        if (response instanceof Response) {
            return ResponseWebSocketMessage.builder()
                    .response((Response) response)
                    .requestId(id)
                    .build();
        } else if (response instanceof ErrorResponse) {
            return ErrorResponseWebSocketMessage.builder()
                    .response((ErrorResponse) response)
                    .requestId(id)
                    .build();
        } else {
            throw new IllegalArgumentException("WebSocketMessage was of unknown type");
        }
    }

    protected abstract MethodResponse[] dispatch(
            final MethodCall methodCall,
            final ListMultimap<String, Response.Invocation> previousResponses
    );

    protected void incrementSessionState() {
        this.sessionState++;
    }

    public enum FailureTrigger {
        NONE, CLOSE, IGNORE, INVALID
    }
}
