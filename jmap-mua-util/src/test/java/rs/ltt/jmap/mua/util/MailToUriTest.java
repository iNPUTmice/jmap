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

package rs.ltt.jmap.mua.util;

import com.google.common.collect.Iterables;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.EmailAddress;

public class MailToUriTest {

    private static final String EXAMPLE_SUBJECT = "Foo";
    private static final String EXAMPLE_ADDRESS_ALPHA = "alpha@example.com";
    private static final String EXAMPLE_ADDRESS_BETA = "beta@example.com";
    private static final String EXAMPLE_ADDRESS_GAMMA = "gamma@example.com";
    private static final String EXAMPLE_ADDRESS_DELTA = "delta@example.com";
    private static final String EXAMPLE_ADDRESS_AT = "\"@\"@example.com";

    @Test
    public void standaloneEmail() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email(EXAMPLE_ADDRESS_ALPHA).build())
                        .build(),
                MailToUri.get(String.format("mailto:%s", EXAMPLE_ADDRESS_ALPHA)));
    }

    @Test
    public void standaloneEmailAddressWithAt() {
        final MailToUri uri = MailToUri.get(String.format("mailto:%s", EXAMPLE_ADDRESS_AT));
        final Collection<EmailAddress> to = uri.getTo();
        Assertions.assertEquals(1, to.size(), "Unexpected number of email addresses in URI");
        Assertions.assertEquals(
                EXAMPLE_ADDRESS_AT,
                Objects.requireNonNull(Iterables.getFirst(to, null)).getEmail());
    }

    @Test
    public void emailAndSubject() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email(EXAMPLE_ADDRESS_ALPHA).build())
                        .subject(EXAMPLE_SUBJECT)
                        .build(),
                MailToUri.get(
                        String.format(
                                "mailto:%s?subject=%s", EXAMPLE_ADDRESS_ALPHA, EXAMPLE_SUBJECT)));
    }

    @Test
    public void emptyToAndSubject() {
        Assertions.assertEquals(
                MailToUri.builder().subject(EXAMPLE_SUBJECT).build(),
                MailToUri.get(String.format("mailto:?subject=%s", EXAMPLE_SUBJECT)));
    }

    @Test
    public void withCcAndBcc() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email(EXAMPLE_ADDRESS_ALPHA).build())
                        .cc(EmailAddress.builder().email(EXAMPLE_ADDRESS_BETA).build())
                        .cc(EmailAddress.builder().email(EXAMPLE_ADDRESS_GAMMA).build())
                        .bcc((EmailAddress.builder().email(EXAMPLE_ADDRESS_DELTA).build()))
                        .subject(EXAMPLE_SUBJECT)
                        .build(),
                MailToUri.get(
                        String.format(
                                "mailto:%s?cc=%s,%s&bcc=%s&subject=%s",
                                EXAMPLE_ADDRESS_ALPHA,
                                EXAMPLE_ADDRESS_BETA,
                                EXAMPLE_ADDRESS_GAMMA,
                                EXAMPLE_ADDRESS_DELTA,
                                EXAMPLE_SUBJECT)));
    }

    @Test
    public void withInReplyTo() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email("list@example.com").build())
                        .inReplyTo("<3469A91.D10AF4C@example.com>")
                        .build(),
                MailToUri.get(
                        "mailto:list@example.com?In-Reply-To=%3C3469A91.D10AF4C@example.com%3E"));
    }

    @Test
    public void specialCharacterEmailAddress() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email("\",\"@example.com").build())
                        .build(),
                MailToUri.get("mailto:\",\"@example.com"));
    }

    @Test
    public void unknownScheme() {
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> MailToUri.get("xmpp:test@example.com"));
    }

    @Test
    public void noScheme() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MailToUri.get("mailto"));
    }

    @Test
    public void emptyQuery() {
        Assertions.assertEquals(
                MailToUri.builder()
                        .to(EmailAddress.builder().email(EXAMPLE_ADDRESS_ALPHA).build())
                        .build(),
                MailToUri.get(String.format("mailto:%s?", EXAMPLE_ADDRESS_ALPHA)));
    }

    @Test
    public void emptyMailto() {
        Assertions.assertEquals(MailToUri.builder().build(), MailToUri.get("mailto:"));
    }

    @Test
    public void emptyMailtoEmptyQuery() {
        Assertions.assertEquals(MailToUri.builder().build(), MailToUri.get("mailto:?"));
    }

    @Test
    public void namedEmailAddressInMailto() throws UnsupportedEncodingException {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        MailToUri.get(
                                String.format(
                                        "mailto:%s",
                                        URLEncoder.encode(
                                                EmailAddressUtil.toString(
                                                        EmailAddress.builder()
                                                                .name("Alpha")
                                                                .email(EXAMPLE_ADDRESS_ALPHA)
                                                                .build()),
                                                "UTF-8"))));
    }
}
