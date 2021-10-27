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
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.EmailAddress;

public class EmailAddressTokenizerTest {

    @Test
    public void emptySequence() {
        Assertions.assertEquals(Collections.emptyList(), EmailAddressTokenizer.tokenize(""));
    }

    @Test
    public void singleChar() {
        final String input = "s";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(0, 0, EmailAddress.builder().email("s").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void whitespaces() {
        final String input = "     ";
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);
        Assertions.assertArrayEquals(
                new EmailAddressToken[0], actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void simpleStandAloneEmailAddress() {
        final String input = "test@example.com";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0, 15, EmailAddress.builder().email("test@example.com").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void singleEmailWithExcessWhiteSpaceAfterDelimiter() {
        final String input = "test@example.com,  ";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0, 16, EmailAddress.builder().email("test@example.com").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void simpleCommaSeparated() {
        final String input = "a@example.com, b@example.com";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0, 13, EmailAddress.builder().email("a@example.com").build()),
                        new EmailAddressToken(
                                14, 27, EmailAddress.builder().email("b@example.com").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void bracketedCommaSeparated() {
        final String input = "<a@example.com>, <b@example.com>";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0, 15, EmailAddress.builder().email("a@example.com").build()),
                        new EmailAddressToken(
                                16, 31, EmailAddress.builder().email("b@example.com").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void singleEmailWithLabel() {
        final String input = "A <a@example.com>";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0,
                                16,
                                EmailAddress.builder().email("a@example.com").name("A").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void singleEmailWithLabelExplicitDelimiter() {
        final String input = "A <a@example.com>";
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input, true);
        Assertions.assertArrayEquals(
                new EmailAddressToken[0], actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void singleEmailWithLabelExcessWhiteSpace() {
        final String input = " A   <a@example.com>";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0,
                                19,
                                EmailAddress.builder().email("a@example.com").name("A").build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void multipleWithQuotedLabel() {
        final String input = "\"Last, First\" <first.last@example.com>, Test <test@example.com>";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0,
                                38,
                                EmailAddress.builder()
                                        .email("first.last@example.com")
                                        .name("Last, First")
                                        .build()),
                        new EmailAddressToken(
                                39,
                                62,
                                EmailAddress.builder()
                                        .email("test@example.com")
                                        .name("Test")
                                        .build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }

    @Test
    public void multipleWithQuotedLabelExplicitDelimiter() {
        final String input = "\"Last, First\" <first.last@example.com>, Test <test@example.com>";
        final Collection<EmailAddressToken> expected =
                ImmutableList.of(
                        new EmailAddressToken(
                                0,
                                38,
                                EmailAddress.builder()
                                        .email("first.last@example.com")
                                        .name("Last, First")
                                        .build()));
        final Collection<EmailAddressToken> actual = EmailAddressTokenizer.tokenize(input, true);

        Assertions.assertArrayEquals(
                expected.toArray(new EmailAddressToken[0]),
                actual.toArray(new EmailAddressToken[0]));
    }
}
