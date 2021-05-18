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

package rs.ltt.jmap.mua.cache;

import com.google.common.base.MoreObjects;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.AddedItem;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.common.method.response.standard.QueryChangesMethodResponse;

import java.util.List;

public class QueryUpdate<T extends AbstractIdentifiableEntity, U> extends AbstractUpdate<T> {

    private final String[] removed;

    private final List<AddedItem<U>> added;

    private final Long total;


    private QueryUpdate(final TypedState<T> oldState,
                        final TypedState<T> newState,
                        final String[] removed,
                        final List<AddedItem<U>> added,
                        final Long total) {
        super(oldState, newState, false);
        this.removed = removed;
        this.added = added;
        this.total = total;
    }

    public static <T extends AbstractIdentifiableEntity, U> QueryUpdate<T, U> of(QueryChangesMethodResponse<T> queryChangesMethodResponse, List<AddedItem<U>> added) {
        return new QueryUpdate<>(
                queryChangesMethodResponse.getOldTypedQueryState(),
                queryChangesMethodResponse.getNewTypedQueryState(),
                queryChangesMethodResponse.getRemoved(),
                added,
                queryChangesMethodResponse.getTotal()
        );
    }

    public String[] getRemoved() {
        return this.removed;
    }

    public List<AddedItem<U>> getAdded() {
        return this.added;
    }

    public Long getTotal() {
        return this.total;
    }

    @Override
    public boolean hasChanges() {
        final boolean modifiedItems =  removed.length + added.size() > 0;
        return modifiedItems || hasStateChange();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("removed", removed)
                .add("added", added)
                .toString();
    }
}
