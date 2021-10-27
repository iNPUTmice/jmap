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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.*;
import rs.ltt.jmap.common.entity.EmailAddress;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class MailToUri {

    private static final String MAIL_TO = "mailto";
    private static final String TO = "to";
    private static final String CC = "cc";
    private static final String BCC = "bcc";
    private static final String IN_REPLY_TO = "in-reply-to";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";

    @Singular("to")
    private final Collection<EmailAddress> to;

    @Singular("cc")
    private final Collection<EmailAddress> cc;

    @Singular("bcc")
    private final Collection<EmailAddress> bcc;

    private final String inReplyTo;
    private final String subject;
    private final String body;

    @Nullable
    public static MailToUri parse(final String input) {
        try {
            return get(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nonnull
    public static MailToUri get(final String input) throws IllegalArgumentException {
        return get(input, true);
    }

    @Nonnull
    public static MailToUri get(final String input, final boolean stripNames)
            throws IllegalArgumentException {
        final int schemeDelimiter = input.indexOf(":");
        if (schemeDelimiter < 0) {
            throw new IllegalArgumentException("No scheme detected");
        }
        if (input.substring(0, schemeDelimiter).equals(MAIL_TO)) {
            final int queryDelimiter =
                    input.length() > schemeDelimiter ? input.indexOf("?", schemeDelimiter + 1) : -1;
            final String to;
            final String query;
            if (queryDelimiter > 0) {
                to = input.substring(schemeDelimiter + 1, queryDelimiter);
                query = input.substring(queryDelimiter + 1);
            } else {
                to = input.substring(schemeDelimiter + 1);
                query = null;
            }
            final Map<String, String> parameters = parseQuery(query);
            final String cc = parameters.get(CC);
            final String bcc = parameters.get(BCC);
            final String inReplyTo = parameters.get(IN_REPLY_TO);
            final String subject = parameters.get(SUBJECT);
            final String body = parameters.get(BODY);
            final MailToUriBuilder mailToUriBuilder = MailToUri.builder();
            if (Strings.isNullOrEmpty(to)) {
                final String toParameter = parameters.get(TO);
                if (toParameter != null) {
                    mailToUriBuilder.to(parseEmailAddress(toParameter, stripNames));
                }
            } else {
                final Collection<EmailAddress> addresses = EmailAddressUtil.parse(decode(to));
                throwOnName(addresses);
                mailToUriBuilder.to(addresses);
            }
            if (cc != null) {
                mailToUriBuilder.cc(parseEmailAddress(cc, stripNames));
            }
            if (bcc != null) {
                mailToUriBuilder.bcc(parseEmailAddress(bcc, stripNames));
            }
            mailToUriBuilder.inReplyTo(inReplyTo);
            mailToUriBuilder.subject(subject);
            mailToUriBuilder.body(body);
            return mailToUriBuilder.build();
        }
        throw new IllegalArgumentException("Unknown scheme");
    }

    private static void throwOnName(final Collection<EmailAddress> addresses)
            throws IllegalArgumentException {
        for (final EmailAddress address : addresses) {
            if (Strings.isNullOrEmpty(address.getName())) {
                continue;
            }
            throw new IllegalArgumentException("Mailto address must not have a name");
        }
    }

    private static Map<String, String> parseQuery(final String query) {
        if (query == null) {
            return Collections.emptyMap();
        }
        final ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        for (final String parameter : query.split("&")) {
            final String[] parts = parameter.split("=", 2);
            if (parts.length == 2) {
                mapBuilder.put(parts[0].toLowerCase(Locale.ENGLISH), decode(parts[1]));
            }
        }
        return mapBuilder.build();
    }

    private static String decode(String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Collection<EmailAddress> parseEmailAddress(
            final String address, final boolean stripNames) {
        return stripNames
                ? stripNames(EmailAddressUtil.parse(address))
                : EmailAddressUtil.parse(address);
    }

    private static Collection<EmailAddress> stripNames(Collection<EmailAddress> emailAddresses) {
        return Collections2.transform(
                emailAddresses,
                new Function<EmailAddress, EmailAddress>() {
                    @Nullable
                    @Override
                    public EmailAddress apply(@Nullable EmailAddress emailAddress) {
                        if (emailAddress == null || Strings.isNullOrEmpty(emailAddress.getName())) {
                            return emailAddress;
                        } else {
                            return EmailAddress.builder().email(emailAddress.getEmail()).build();
                        }
                    }
                });
    }
}
