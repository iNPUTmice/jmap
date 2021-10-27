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
import lombok.Getter;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.common.method.MethodResponse;

@Getter
public abstract class ChangesMethodResponse<T extends AbstractIdentifiableEntity>
        implements MethodResponse {

    protected String accountId;
    protected String oldState;
    protected String newState;
    protected boolean hasMoreChanges;
    protected String[] created;
    protected String[] updated;
    protected String[] destroyed;

    public ChangesMethodResponse(
            String accountId,
            String oldState,
            String newState,
            boolean hasMoreChanges,
            String[] created,
            String[] updated,
            String[] destroyed) {
        this.accountId = accountId;
        this.oldState = oldState;
        this.newState = newState;
        this.hasMoreChanges = hasMoreChanges;
        this.created = created;
        this.updated = updated;
        this.destroyed = destroyed;
    }

    public TypedState<T> getTypedOldState() {
        return TypedState.of(this.oldState);
    }

    public TypedState<T> getTypedNewState() {
        return TypedState.of(this.newState);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("oldState", oldState)
                .add("newState", newState)
                .add("hasMoreChanges", hasMoreChanges)
                .add("created", created)
                .add("updated", updated)
                .add("destroyed", destroyed)
                .toString();
    }
}
