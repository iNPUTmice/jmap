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

package rs.ltt.jmap.mua;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import rs.ltt.jmap.common.entity.IdentifiableEmailWithMailboxIds;

class MyIdentifiableEmailWithMailboxes implements IdentifiableEmailWithMailboxIds {

    private final String id;
    private final Set<String> mailboxIds;

    MyIdentifiableEmailWithMailboxes(String id, String mailboxId) {
        this.id = id;
        this.mailboxIds = ImmutableSet.of(mailboxId);
    }

    MyIdentifiableEmailWithMailboxes(String id, Set<String> mailboxIds) {
        this.id = id;
        this.mailboxIds = mailboxIds;
    }

    @Override
    public Map<String, Boolean> getMailboxIds() {
        return Maps.asMap(mailboxIds, s -> true);
    }

    @Override
    public String getId() {
        return id;
    }
}
