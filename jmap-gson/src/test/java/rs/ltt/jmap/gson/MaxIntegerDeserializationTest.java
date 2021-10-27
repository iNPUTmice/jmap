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

import com.google.common.math.LongMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Mailbox;

public class MaxIntegerDeserializationTest extends AbstractGsonTest {

    @Test
    public void emailDeserialization() throws Exception {
        final Email email = parseFromResource("email/max-int-email.json", Email.class);
        Assertions.assertEquals((long) email.getSize(), LongMath.pow(2, 53) - 1L);
        Assertions.assertEquals(
                (long) email.getTextBody().get(0).getSize(), LongMath.pow(2, 53) - 2L);
    }

    @Test
    public void mailboxDeserialization() throws Exception {
        final Mailbox mailbox = parseFromResource("mailbox/max-int-mailbox.json", Mailbox.class);
        Assertions.assertEquals((long) mailbox.getTotalEmails(), LongMath.pow(2, 53) - 1L);
        Assertions.assertEquals((long) mailbox.getUnreadEmails(), LongMath.pow(2, 53) - 2L);
    }
}
