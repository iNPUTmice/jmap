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

package rs.ltt.jmap.common;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.SubmissionAccountCapability;
import rs.ltt.jmap.common.entity.capability.WebSocketCapability;

public class SessionResourceTest {


    @Test
    public void wrongAccountCapability() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SessionResource.builder()
                .apiUrl("/jmap/")
                .state("initial")
                .account("user@example.com", Account.builder()
                        .accountCapabilities(ImmutableMap.of(SubmissionAccountCapability.class, MailAccountCapability.builder().build()))
                        .build())
                .primaryAccounts(ImmutableMap.of(MailAccountCapability.class, "user@example.com"))
                .build());
    }

    @Test
    public void correctAccountCapability() {
        SessionResource.builder()
                .apiUrl("/jmap/")
                .state("initial")
                .account("user@example.com", Account.builder()
                        .accountCapabilities(ImmutableMap.of(MailAccountCapability.class, MailAccountCapability.builder().build()))
                        .build())
                .primaryAccounts(ImmutableMap.of(MailAccountCapability.class, "user@example.com"))
                .build();
    }

    @Test
    public void wrongCapability() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SessionResource.builder()
                .apiUrl("/jmap/")
                .state("initital")
                .capabilities(ImmutableMap.of(CoreCapability.class, WebSocketCapability.builder().build()))
                .build());
    }

    @Test
    public void correctCapability() {
        Assertions.assertNotNull(SessionResource.builder()
                .apiUrl("/jmap/")
                .state("initital")
                .capabilities(ImmutableMap.of(CoreCapability.class, CoreCapability.builder().build()))
                .build());
    }

}
