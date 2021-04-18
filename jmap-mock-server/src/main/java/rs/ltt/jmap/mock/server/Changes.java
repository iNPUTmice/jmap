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

package rs.ltt.jmap.mock.server;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashSet;

public class Changes {

    public final String[] updated;
    public final String[] created;

    public Changes(String[] updated, String[] created) {
        this.updated = updated;
        this.created = created;
    }

    public static Changes merge(Collection<Changes> changes) {
        final HashSet<String> updated = new HashSet<>();
        final HashSet<String> created = new HashSet<>();
        for(final Changes change : changes) {
            created.addAll(ImmutableSet.copyOf(change.created));
            for (final String id : change.updated) {
                if (created.contains(id)) {
                    continue;
                }
                updated.add(id);
            }
        }
        return new Changes(updated.toArray(new String[0]), created.toArray(new String[0]));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("updated", updated)
                .add("created", created)
                .toString();
    }
}
