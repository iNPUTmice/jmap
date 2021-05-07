/*
 * Copyright 2021 Daniel Gultsch
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

package rs.ltt.jmap.gson;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonIOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;

import java.util.UUID;

public class ResponseSerializationTest extends AbstractGsonTest {

    @Test
    public void customNotAnnotatedMethodError() {
        final ImmutableList.Builder<Response.Invocation> responseInvocations = ImmutableList.builder();
        responseInvocations.add(new Response.Invocation(new CustomMethodError(), UUID.randomUUID().toString()));
        Response response = new Response(
                responseInvocations.build().toArray(new Response.Invocation[0]),
                "session-state-01"
        );
        final JsonIOException jsonIOException = Assertions.assertThrows(JsonIOException.class, () -> {
            getGson().toJson(response);
        });
        Assertions.assertEquals(
                "Unable to serialize CustomMethodError. Did you annotate the Method with @JmapError?",
                jsonIOException.getMessage()
        );

    }

    @Test
    public void customNotAnnotatedMethodResponse() {
        final ImmutableList.Builder<Response.Invocation> responseInvocations = ImmutableList.builder();
        responseInvocations.add(new Response.Invocation(new CustomMethodResponse(), UUID.randomUUID().toString()));
        Response response = new Response(
                responseInvocations.build().toArray(new Response.Invocation[0]),
                "session-state-01"
        );
        final JsonIOException jsonIOException = Assertions.assertThrows(JsonIOException.class, () -> {
            getGson().toJson(response);
        });
        Assertions.assertEquals(
                "Unable to serialize CustomMethodResponse. Did you annotate the method with @JmapMethod?",
                jsonIOException.getMessage()
        );

    }

    public static class CustomMethodResponse implements MethodResponse {

    }

    public static class CustomMethodError extends MethodErrorResponse {

    }


}
