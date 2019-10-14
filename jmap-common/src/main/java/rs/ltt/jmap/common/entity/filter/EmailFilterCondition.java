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

package rs.ltt.jmap.common.entity.filter;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.util.IndexableStringUtils;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@Builder
public class EmailFilterCondition implements FilterCondition<Email> {

    private String inMailbox;

    private String[] inMailboxOtherThan;

    private Long minSize;

    private Long maxSize;

    private String text;

    private String from;

    private String to;

    private String cc;

    private String bcc;

    private String subject;

    private String body;

    @Override
    public String toQueryString() {
        return IndexableStringUtils.toIndexableString(L3_DIVIDER, L4_DIVIDER, inMailbox, inMailboxOtherThan, minSize, maxSize, text, from, to, cc, bcc, subject, body);
    }

    @Override
    public int compareTo(@NonNullDecl Filter<Email> filter) {
        if (filter instanceof EmailFilterCondition) {
            EmailFilterCondition other = (EmailFilterCondition) filter;
            return ComparisonChain.start()
                    .compare(Strings.nullToEmpty(inMailbox), Strings.nullToEmpty(other.inMailbox))
                    .compare(inMailboxOtherThan, other.inMailboxOtherThan, new IndexableStringUtils.StringArrayComparator())
                    .compare(minSize == null ? 0L : minSize, other.minSize == null ? 0L : other.minSize)
                    .compare(maxSize == null ? 0L : maxSize, other.maxSize == null ? 0L : other.maxSize)
                    .compare(Strings.nullToEmpty(text), Strings.nullToEmpty(other.text))
                    .compare(Strings.nullToEmpty(from), Strings.nullToEmpty(other.from))
                    .compare(Strings.nullToEmpty(cc), Strings.nullToEmpty(other.cc))
                    .compare(Strings.nullToEmpty(bcc), Strings.nullToEmpty(other.bcc))
                    .compare(Strings.nullToEmpty(subject), Strings.nullToEmpty(other.subject))
                    .compare(Strings.nullToEmpty(body), Strings.nullToEmpty(other.body))
                    .result();
        } else {
            return 1;
        }
    }
}
