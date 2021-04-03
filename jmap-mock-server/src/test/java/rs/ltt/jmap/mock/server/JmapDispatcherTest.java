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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.Version;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.entity.ErrorType;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class JmapDispatcherTest {

    private static final Gson GSON;

    private static final String INDEX_0_USERNAME = "dorothy.williams@example.com";
    private static final String INDEX_1_USERNAME = "ruth.brown@example.com";

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    @Test
    public void wrongContentType() throws IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        final StubMailServer stubMailServer = new StubMailServer();
        mockWebServer.setDispatcher(stubMailServer);

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();


        final Response response = okHttpClient.newCall(new Request.Builder()
                .url(mockWebServer.url("/jmap/"))
                .addHeader("Authorization", Credentials.basic(INDEX_0_USERNAME, StubMailServer.PASSWORD))
                .post(RequestBody.create("{}", MediaType.get("text/plain")))
                .build()).execute();

        Assertions.assertEquals(400, response.code());

        final ResponseBody body = response.body();

        Assertions.assertNotNull(body);

        final GenericResponse genericResponse = GSON.fromJson(body.string(), GenericResponse.class);

        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));

        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(ErrorType.NOT_JSON, errorResponse.getType());

        mockWebServer.shutdown();
    }

    @Test
    public void unauthorized() throws IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new StubMailServer());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();


        final Response response = okHttpClient.newCall(new Request.Builder()
                .url(mockWebServer.url("/jmap/"))
                .addHeader("Authorization", Credentials.basic(INDEX_0_USERNAME, "wrong!"))
                .post(RequestBody.create("{}", MediaType.get("application/json")))
                .build()).execute();

        Assertions.assertEquals(401, response.code());

        mockWebServer.shutdown();
    }

    @Test
    public void notRequest() throws IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new StubMailServer());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();


        final Response response = okHttpClient.newCall(new Request.Builder()
                .url(mockWebServer.url("/jmap/"))
                .addHeader("Authorization", Credentials.basic(INDEX_0_USERNAME, StubMailServer.PASSWORD))
                .post(RequestBody.create("{}", MediaType.get("application/json")))
                .build()).execute();

        Assertions.assertEquals(400, response.code());

        final ResponseBody body = response.body();

        Assertions.assertNotNull(body);

        final GenericResponse genericResponse = GSON.fromJson(body.string(), GenericResponse.class);

        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));

        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(ErrorType.NOT_REQUEST, errorResponse.getType());

        mockWebServer.shutdown();
    }

    @Test
    public void invalidJson() throws IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new StubMailServer());

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();


        final Response response = okHttpClient.newCall(new Request.Builder()
                .url(mockWebServer.url("/jmap/"))
                .addHeader("Authorization", Credentials.basic(INDEX_0_USERNAME, StubMailServer.PASSWORD))
                .post(RequestBody.create("{}}", MediaType.get("application/json")))
                .build()).execute();

        Assertions.assertEquals(400, response.code());

        final ResponseBody body = response.body();

        Assertions.assertNotNull(body);

        final GenericResponse genericResponse = GSON.fromJson(body.string(), GenericResponse.class);

        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));

        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(ErrorType.NOT_JSON, errorResponse.getType());

        mockWebServer.shutdown();
    }

    @Test
    public void echo() throws IOException, ExecutionException, InterruptedException {
        final MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new StubMailServer());

        final JmapClient jmapClient = new JmapClient(
                INDEX_0_USERNAME,
                StubMailServer.PASSWORD,
                mockWebServer.url(StubMailServer.WELL_KNOWN_PATH)
        );

        final EchoMethodResponse response = jmapClient.call(
                EchoMethodCall.builder().libraryName(Version.getUserAgent()).build()
        ).get().getMain(EchoMethodResponse.class);
        Assertions.assertEquals(Version.getUserAgent(), response.getLibraryName());
        mockWebServer.shutdown();
    }

    @Test
    public void secondAccountEcho() throws IOException, ExecutionException, InterruptedException {
        final MockWebServer mockWebServer = new MockWebServer();
        final StubMailServer stubMailServer = new StubMailServer(1);
        mockWebServer.setDispatcher(stubMailServer);

        final JmapClient jmapClient = new JmapClient(
                INDEX_1_USERNAME,
                StubMailServer.PASSWORD,
                mockWebServer.url(StubMailServer.WELL_KNOWN_PATH)
        );

        final EchoMethodResponse response = jmapClient.call(
                EchoMethodCall.builder().libraryName(Version.getUserAgent()).build()
        ).get().getMain(EchoMethodResponse.class);
        Assertions.assertEquals(Version.getUserAgent(), response.getLibraryName());
        mockWebServer.shutdown();
    }
}