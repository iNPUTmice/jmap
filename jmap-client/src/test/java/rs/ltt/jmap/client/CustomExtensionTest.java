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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.dummy.SetDummyMethodCall;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.common.util.Mapper;
import rs.ltt.jmap.gson.JmapAdapters;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static rs.ltt.jmap.client.HttpJmapClientTest.WELL_KNOWN_PATH;
import static rs.ltt.jmap.client.HttpJmapClientTest.readResourceAsString;

public class CustomExtensionTest {

    private static final String ACCOUNT_ID = "test@example.com";
    private static final String USERNAME = "test@example.com";
    private static final String PASSWORD = "secret";

    private static final String EXPECTED_JSON_QUERY_CALL = "{\"accountId\":\"accountId\",\"filter\":{\"isPlaceholder\":true}}";

    @Test
    public void findDummyAndCommonMethodCalls() {
        Assertions.assertTrue(Mapper.METHOD_CALLS.containsValue(GetDummyMethodCall.class));
        Assertions.assertTrue(Mapper.METHOD_CALLS.containsValue(EchoMethodCall.class));
    }

    @Test
    public void findDummyAndCommonMethodResponses() {
        Assertions.assertTrue(Mapper.METHOD_RESPONSES.containsValue(GetDummyMethodResponse.class));
        Assertions.assertTrue(Mapper.METHOD_RESPONSES.containsValue(EchoMethodResponse.class));
    }

    @Test
    public void failOnCallWithoutNamespace() {
        final JmapClient client = new JmapClient(USERNAME, PASSWORD);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            client.call(new GetDummyMethodCall(ACCOUNT_ID)).get();
        });

    }

    @Test
    public void notAnnotatedSet() throws IOException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(readResourceAsString("fetch-mailboxes/01-session.json")));
        server.start();

        final JmapClient client = new JmapClient(
                USERNAME,
                PASSWORD,
                server.url(WELL_KNOWN_PATH)
        );
        final ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> {
            client.call(new SetDummyMethodCall(
                    ACCOUNT_ID,
                    null,
                    null,
                    null,
                    null,
                    null
            )).get();
        });
        MatcherAssert.assertThat(executionException.getCause(), CoreMatchers.instanceOf(JsonIOException.class));
    }

    @Test
    public void serializeQuery() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        final Gson gson = gsonBuilder.create();
        final QueryDummyMethodCall queryDummyMethodCall = new QueryDummyMethodCall(
                "accountId",
                new DummyFilterCondition(true),
                null,
                null,
                null,
                null,
                null,
                null
        );
        Assertions.assertEquals(EXPECTED_JSON_QUERY_CALL, gson.toJson(queryDummyMethodCall));
    }

    @Test
    public void deserializeQuery() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        final Gson gson = gsonBuilder.create();
        final QueryDummyMethodCall queryMethodCall = gson.fromJson(EXPECTED_JSON_QUERY_CALL, QueryDummyMethodCall.class);
        MatcherAssert.assertThat(queryMethodCall.getFilter(), CoreMatchers.instanceOf(DummyFilterCondition.class));
    }

}
