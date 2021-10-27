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

package rs.ltt.jmap.gson;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.method.call.email.CopyEmailMethodCall;

public class CopyEmailMethodCallTest extends AbstractGsonTest {

    @Test
    public void copyEmailMethodCall() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        Gson gson = builder.create();
        CopyEmailMethodCall copyEmailMethodCall =
                CopyEmailMethodCall.builder()
                        .fromAccountId("from@domain.tld")
                        .accountId("to@domain.tld")
                        .create(ImmutableMap.of("a", Email.of("M1001")))
                        .build();
        Request request = new Request.Builder().call(copyEmailMethodCall).build();
        Assertions.assertEquals(
                readResourceAsString("request/copy-email.json"), gson.toJson(request));
    }
}
