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

import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;

public class RfcExamplesDeserializerTest extends AbstractGsonTest {

    @Test
    public void emailGetResponse() throws IOException {
        Response.Invocation[] responseInvocation =
                parseFromResource(
                        "rfc-example/email-get-response.json", Response.Invocation[].class);
        Assertions.assertEquals(responseInvocation.length, 1);
        MatcherAssert.assertThat(
                responseInvocation[0].getMethodResponse(),
                instanceOf(GetEmailMethodResponse.class));
        final GetEmailMethodResponse methodResponse =
                (GetEmailMethodResponse) responseInvocation[0].getMethodResponse();
        final Email[] emails = methodResponse.getList();
        Assertions.assertEquals(emails.length, 1);
        final Email email = emails[0];
        Assertions.assertEquals(email.getId(), "f123u457");
        Assertions.assertEquals(email.getBodyValues().size(), 2);
        Assertions.assertEquals(email.getFrom().size(), 1);
        Assertions.assertEquals(email.getSubject(), "Dinner on Thursday?");
        Assertions.assertEquals(email.getReceivedAt(), email.getSentAt().toInstant());
    }

    @Test
    public void identityGetResponse() throws IOException {
        Response.Invocation invocation =
                parseFromResource(
                        "rfc-example/identity-get-response.json", Response.Invocation.class);
        MatcherAssert.assertThat(
                invocation.getMethodResponse(), instanceOf(GetIdentityMethodResponse.class));
        GetIdentityMethodResponse methodResponse =
                (GetIdentityMethodResponse) invocation.getMethodResponse();
        Identity[] identities = methodResponse.getList();
        Assertions.assertEquals(identities.length, 2);
        Assertions.assertEquals(identities[0].getName(), "Joe Bloggs");
    }
}
