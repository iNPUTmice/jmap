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

package rs.ltt.jmap.common.method.response.standard;

import com.google.common.base.MoreObjects;
import java.util.Map;
import lombok.Getter;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.method.MethodResponse;

@Getter
public abstract class SetMethodResponse<T extends AbstractIdentifiableEntity>
        implements MethodResponse {

    private String accountId;
    private String oldState;
    private String newState;
    private Map<String, T> created;
    private Map<String, T> updated;
    private String[] destroyed;
    private Map<String, SetError> notCreated;
    private Map<String, SetError> notUpdated;
    private Map<String, SetError> notDestroyed;

    public SetMethodResponse(
            String accountId,
            String oldState,
            String newState,
            Map<String, T> created,
            Map<String, T> updated,
            String[] destroyed,
            Map<String, SetError> notCreated,
            Map<String, SetError> notUpdated,
            Map<String, SetError> notDestroyed) {
        this.accountId = accountId;
        this.oldState = oldState;
        this.newState = newState;
        this.created = created;
        this.updated = updated;
        this.destroyed = destroyed;
        this.notCreated = notCreated;
        this.notUpdated = notUpdated;
        this.notDestroyed = notDestroyed;
    }

    public int getUpdatedCreatedCount() {
        return (created == null ? 0 : created.size()) + (updated == null ? 0 : updated.size());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("oldState", oldState)
                .add("newState", newState)
                .add("created", created)
                .add("updated", updated)
                .add("destroyed", destroyed)
                .add("notCreated", notCreated)
                .add("notUpdated", notUpdated)
                .add("notDestroyed", notDestroyed)
                .toString();
    }
}
