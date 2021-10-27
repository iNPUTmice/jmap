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
import rs.ltt.jmap.common.entity.EmailSubmission;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.method.call.submission.SetEmailSubmissionMethodCall;
import rs.ltt.jmap.common.util.Patches;

public class SetEmailSubmissionMethodCallTest extends AbstractGsonTest {

    @Test
    public void setEmailSubmissionTest() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        Gson gson = builder.create();

        final Patches.Builder patchesBuilder = Patches.builder();
        patchesBuilder.remove("keywords/" + Keyword.DRAFT);
        patchesBuilder.set("mailboxIds/MB3", true);
        SetEmailSubmissionMethodCall submissionCall =
                SetEmailSubmissionMethodCall.builder()
                        .accountId("accountId")
                        .create(
                                ImmutableMap.of(
                                        "es0",
                                        EmailSubmission.builder()
                                                .emailId("M1234")
                                                .identityId("I0")
                                                .build()))
                        .onSuccessUpdateEmail(ImmutableMap.of("#es0", patchesBuilder.build()))
                        .build();
        Request request = new Request.Builder().call(submissionCall).build();
        Assertions.assertEquals(
                readResourceAsString("request/set-email-submission.json"), gson.toJson(request));
    }

    @Test
    public void setEmailSubmissionNoImplicitEmailUpdate() throws IOException {
        final GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        final Gson gson = builder.create();

        final Patches.Builder patchesBuilder = Patches.builder();
        patchesBuilder.remove("keywords/" + Keyword.DRAFT);
        patchesBuilder.set("mailboxIds/MB3", true);
        SetEmailSubmissionMethodCall submissionCall =
                SetEmailSubmissionMethodCall.builder()
                        .accountId("accountId")
                        .create(
                                ImmutableMap.of(
                                        "es0",
                                        EmailSubmission.builder()
                                                .emailId("M1234")
                                                .identityId("I0")
                                                .build()))
                        .build();
        final Request request = new Request.Builder().call(submissionCall).build();
        Assertions.assertEquals(
                readResourceAsString("request/set-email-submission-no-implicit.json"),
                gson.toJson(request));
    }
}
