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
public abstract class QueryMethodResponse<T extends AbstractIdentifiableEntity> implements MethodResponse {
    private String accountId;
    private String queryState;
    private boolean canCalculateChanges;
    private Long position;
    private String[] ids;
    private Long total;
    private Long limit;

    public TypedState<T> getTypedQueryState() {
        return TypedState.of(queryState);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountId", accountId)
                .add("queryState", queryState)
                .add("canCalculateChanges", canCalculateChanges)
                .add("position", position)
                .add("ids", ids)
                .add("total", total)
                .add("limit", limit)
                .toString();
    }
}
