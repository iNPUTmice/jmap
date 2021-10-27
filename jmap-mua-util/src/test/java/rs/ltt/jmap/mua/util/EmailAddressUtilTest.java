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

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.EmailAddress;

public class EmailAddressUtilTest {

    @Test
    public void twoEmailAddressesWithLabel() {
        final String input = "\"Last, First\" <first.last@example.com>, Test <test@example.com>";
        final Collection<EmailAddress> expected =
                ImmutableList.of(
                        EmailAddress.builder()
                                .email("first.last@example.com")
                                .name("Last, First")
                                .build(),
                        EmailAddress.builder().email("test@example.com").name("Test").build());
        final Collection<EmailAddress> actual = EmailAddressUtil.parse(input);
        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddress[0]), actual.toArray(new EmailAddress[0]));
    }

    @Test
    public void validEmailAddresses() {
        // copied from https://en.wikipedia.org/wiki/Email_address#Examples
        Assertions.assertTrue(EmailAddressUtil.isValid("simple@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("very.common@example.com"));
        Assertions.assertTrue(
                EmailAddressUtil.isValid("disposable.style.email.with+symbol@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("other.email-with-hyphen@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("fully-qualified-domain@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("user.name+tag+sorting@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("x@example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("example-indeed@strange-example.com"));
        Assertions.assertTrue(EmailAddressUtil.isValid("admin@mailserver1"));
        Assertions.assertTrue(EmailAddressUtil.isValid("example@s.example"));
        Assertions.assertTrue(EmailAddressUtil.isValid("\" \"@example.org"));
        Assertions.assertTrue(EmailAddressUtil.isValid("\"john..doe\"@example.org"));
        Assertions.assertTrue(EmailAddressUtil.isValid("mailhost!username@example.org"));
        Assertions.assertTrue(EmailAddressUtil.isValid("user%example.com@example.org"));
    }

    @Test
    public void invalidEmailAddresses() {
        // copied from https://en.wikipedia.org/wiki/Email_address#Examples
        Assertions.assertFalse(EmailAddressUtil.isValid("Abc.example.com"));
        Assertions.assertFalse(EmailAddressUtil.isValid("A@b@c@example.com"));
        Assertions.assertFalse(EmailAddressUtil.isValid("a\"b(c)d,e:f;g<h>i[j\\k]l@example.com"));
        Assertions.assertFalse(EmailAddressUtil.isValid("just\"not\"right@example.com"));
        Assertions.assertFalse(EmailAddressUtil.isValid("this is\"not\\allowed@example.com"));
        Assertions.assertFalse(
                EmailAddressUtil.isValid("this\\ still\\\"not\\\\allowed@example.com"));
    }
}
