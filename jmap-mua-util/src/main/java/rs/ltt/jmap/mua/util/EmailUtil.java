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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithAddresses;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithSubject;

public class EmailUtil {

    private static final String RESPONSE_PREFIX = "Re";

    private static final List<String> RESPONSE_PREFIXES = Arrays.asList("re", "aw");

    private EmailUtil() {}

    public static String getResponseSubject(IdentifiableEmailWithSubject emailWithSubject) {
        final String subject = emailWithSubject.getSubject();
        final int length = subject.length();
        if (length <= 3) {
            return subjectWithPrefix(subject);
        }
        final String prefix = subject.substring(0, 3);
        if (prefix.charAt(2) == ':'
                && RESPONSE_PREFIXES.contains(prefix.substring(0, 2).toLowerCase())) {
            return subjectWithPrefix(subject.substring(3));
        }
        return subjectWithPrefix(subject);
    }

    public static String subjectWithPrefix(final String subject) {
        return String.format("%s: %s", RESPONSE_PREFIX, subject.trim());
    }

    public static ReplyAddresses reply(IdentifiableEmailWithAddresses emailWithAddresses) {
        final Collection<EmailAddress> replyTo = emailWithAddresses.getReplyTo();
        if (replyTo != null && replyTo.size() > 0) {
            return new ReplyAddresses(replyTo);
        }
        return new ReplyAddresses(replyTo(emailWithAddresses));
    }

    private static Collection<EmailAddress> replyTo(
            IdentifiableEmailWithAddresses emailWithAddresses) {
        final Collection<EmailAddress> from = emailWithAddresses.getFrom();
        if (from != null && !from.isEmpty()) {
            return from;
        }
        final Collection<EmailAddress> sender = emailWithAddresses.getSender();
        if (sender != null && !sender.isEmpty()) {
            return sender;
        }
        return Collections.emptyList();
    }

    public static ReplyAddresses replyAll(IdentifiableEmailWithAddresses emailWithAddresses) {
        final Collection<EmailAddress> replyTo = emailWithAddresses.getReplyTo();
        final Collection<EmailAddress> cc = emailWithAddresses.getCc();
        if (replyTo != null && replyTo.size() > 0 && (cc == null || cc.isEmpty())) {
            return new ReplyAddresses(replyTo);
        }
        final Collection<EmailAddress> to = emailWithAddresses.getTo();
        ImmutableList.Builder<EmailAddress> ccBuilder = new ImmutableList.Builder<>();
        if (to != null) {
            ccBuilder.addAll(to);
        }
        if (cc != null) {
            ccBuilder.addAll(cc);
        }
        if (replyTo != null && replyTo.size() > 0) {
            return new ReplyAddresses(replyTo, ccBuilder.build());
        } else {
            return new ReplyAddresses(replyTo(emailWithAddresses), ccBuilder.build());
        }
    }

    public static class ReplyAddresses {
        private final Collection<EmailAddress> to;
        private final Collection<EmailAddress> cc;

        public ReplyAddresses(Collection<EmailAddress> to) {
            this.to = to;
            this.cc = Collections.emptyList();
        }

        public ReplyAddresses(Collection<EmailAddress> to, Collection<EmailAddress> cc) {
            this.to = to;
            this.cc = cc;
        }

        public Collection<EmailAddress> getTo() {
            return to;
        }

        public Collection<EmailAddress> getCc() {
            return cc;
        }
    }
}
