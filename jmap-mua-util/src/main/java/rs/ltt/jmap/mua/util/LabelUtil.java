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

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ComparisonChain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import rs.ltt.jmap.common.entity.Role;

public class LabelUtil {

    public static final Comparator<? super Label> COMPARATOR =
            (Comparator<Label>)
                    (a, b) ->
                            ComparisonChain.start()
                                    .compare(order(a.getRole()), (order(b.getRole())))
                                    .compare(
                                            Strings.nullToEmpty(a.getName()),
                                            Strings.nullToEmpty(b.getName()))
                                    .result();
    private static final Collection<KeywordLabel> KEYWORD_LABELS =
            Collections2.transform(
                    KeywordUtil.KEYWORD_ROLE.entrySet(),
                    entry -> new KeywordLabel(entry.getKey(), entry.getValue()));

    public static List<LabelWithCount> fillUpAndSort(List<? extends LabelWithCount> mailboxes) {
        final ArrayList<LabelWithCount> labels = new ArrayList<>(mailboxes);
        for (final KeywordLabel keywordLabel : KEYWORD_LABELS) {
            if (!anyIsRole(mailboxes, keywordLabel.getRole())) {
                labels.add(keywordLabel);
            }
        }
        labels.sort(COMPARATOR);
        return labels;
    }

    private static boolean anyIsRole(final Collection<? extends Label> labels, final Role role) {
        for (final Label label : labels) {
            if (label.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    private static int order(Role role) {
        if (role == null) {
            return 0;
        }
        switch (role) {
            case INBOX:
                return -30;
            case FLAGGED:
                return -20;
            case IMPORTANT:
                return -10;
            case ALL:
                return 10;
            case ARCHIVE:
                return 20;
            case SENT:
                return 30;
            case DRAFTS:
                return 40;
            case JUNK:
                return 50;
            case TRASH:
                return 60;
            default:
                return 70;
        }
    }
}
