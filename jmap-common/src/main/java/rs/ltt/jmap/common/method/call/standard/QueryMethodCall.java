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

package rs.ltt.jmap.common.method.call.standard;

import lombok.Getter;
import lombok.NonNull;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.MethodCall;

@Getter
public abstract class QueryMethodCall<T extends AbstractIdentifiableEntity> implements MethodCall {

    @NonNull private String accountId;

    private Filter<T> filter;

    private Comparator[] sort;

    private Long position;

    private String anchor;

    private Long anchorOffset;

    private Long limit;

    private Boolean calculateTotal;

    public QueryMethodCall(
            @NonNull String accountId,
            Filter<T> filter,
            Comparator[] sort,
            Long position,
            String anchor,
            Long anchorOffset,
            Long limit,
            Boolean calculateTotal) {
        this.accountId = accountId;
        this.filter = filter;
        this.sort = sort;
        this.position = position;
        this.anchor = anchor;
        if (anchor != null) {
            this.anchorOffset = (anchorOffset != null) ? anchorOffset : 1L;
        } else {
            this.anchorOffset = null;
        }
        this.limit = limit;
        this.calculateTotal = calculateTotal;
    }
}
