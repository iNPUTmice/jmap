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

package rs.ltt.jmap.mua.util;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Email;

public class EmailUtilTest {

    @Test
    public void firstResponseSubject() {
        Assertions.assertEquals(
                "Re: Hello",
                EmailUtil.getResponseSubject(Email.builder().subject("Hello").build()));
    }

    @Test
    public void secondResponseSubject() {
        Assertions.assertEquals(
                "Re: Hello",
                EmailUtil.getResponseSubject(Email.builder().subject("RE: Hello").build()));
    }

    @Test
    public void secondResponseSubjectExcessWhitespace() {
        Assertions.assertEquals(
                "Re: Hello",
                EmailUtil.getResponseSubject(Email.builder().subject("RE:    Hello  ").build()));
    }

    @Test
    public void secondResponseSubjectGerman() {
        Assertions.assertEquals(
                "Re: Hello",
                EmailUtil.getResponseSubject(Email.builder().subject("Aw: Hello").build()));
    }

    @Test
    public void effectiveDateSentBeforeReceived() {
        final Instant now = Instant.now();
        final Instant earlier = now.minus(Duration.ofMinutes(10));
        final OffsetDateTime sentAt = earlier.atOffset(ZoneOffset.UTC);
        final Email email = Email.builder().sentAt(sentAt).receivedAt(now).build();
        Assertions.assertEquals(earlier, EmailUtil.getEffectiveDate(email));
    }

    @Test
    public void effectiveDateSentAfterReceived() {
        final Instant now = Instant.now();
        final Instant later = now.plus(Duration.ofMinutes(10));
        final OffsetDateTime sentAt = later.atOffset(ZoneOffset.UTC);
        final Email email = Email.builder().sentAt(sentAt).receivedAt(now).build();
        Assertions.assertEquals(now, EmailUtil.getEffectiveDate(email));
    }
}
