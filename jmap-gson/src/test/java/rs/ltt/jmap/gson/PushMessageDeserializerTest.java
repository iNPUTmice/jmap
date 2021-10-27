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

import java.io.IOException;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.common.entity.PushVerification;
import rs.ltt.jmap.common.entity.StateChange;

public class PushMessageDeserializerTest extends AbstractGsonTest {

    @Test
    public void deserializeStateChange() throws IOException {
        final PushMessage pushMessage =
                parseFromResource("push/state-change.json", PushMessage.class);
        MatcherAssert.assertThat(pushMessage, CoreMatchers.instanceOf(StateChange.class));
        final StateChange stateChange = (StateChange) pushMessage;
        final Map<Class<? extends AbstractIdentifiableEntity>, String> max =
                stateChange.getChanged().get("max@example.com");
        Assertions.assertEquals(2, max.size());
    }

    @Test
    public void deserializeVerification() throws IOException {
        final PushMessage pushMessage =
                parseFromResource("push/verification.json", PushMessage.class);
        MatcherAssert.assertThat(pushMessage, CoreMatchers.instanceOf(PushVerification.class));
        final PushVerification pushVerification = (PushVerification) pushMessage;
        Assertions.assertEquals(
                "P43dcfa4-1dd4-41ef-9156-2c89b3b19c60", pushVerification.getPushSubscriptionId());
        Assertions.assertEquals(
                "da1f097b11ca17f06424e30bf02bfa67", pushVerification.getVerificationCode());
    }
}
