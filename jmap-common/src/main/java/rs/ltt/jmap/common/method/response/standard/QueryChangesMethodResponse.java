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
import rs.ltt.jmap.common.entity.AddedItem;
import rs.ltt.jmap.common.entity.TypedState;
import rs.ltt.jmap.common.method.MethodResponse;

import java.util.List;

@Getter
public abstract class QueryChangesMethodResponse<T extends AbstractIdentifiableEntity> implements MethodResponse {

    private String accountId;
    private String oldQueryState;
    private String newQueryState;
    private long total;
    private String[] removed;
    private List<AddedItem<String>> added;


    public TypedState<T> getOldTypedQueryState() {
        return TypedState.of(oldQueryState);
    }

    public TypedState<T> getNewTypedQueryState() {
        return TypedState.of(newQueryState);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("oldQueryState", oldQueryState)
                .add("newQueryState", newQueryState)
                .add("total", total)
                .add("removed", removed)
                .add("added", added)
                .toString();
    }
}
