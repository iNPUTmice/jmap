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

import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.Comparator;
import rs.ltt.jmap.common.entity.query.Query;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.MethodCall;

public abstract class QueryChangesMethodCall<T extends AbstractIdentifiableEntity> implements MethodCall {

    private String accountId;
    private Filter filter;
    private Comparator[] sort;
    private String sinceQueryState;
    private Long maxChanges;
    private String upToId;
    private Boolean calculateTotal;

    public QueryChangesMethodCall(String sinceQueryState, final Filter<T> filter) {
        this.sinceQueryState = sinceQueryState;
        this.filter = filter;
    }

    public QueryChangesMethodCall(String sinceQueryState, final Query<T> query) {
        this.sinceQueryState = sinceQueryState;
        this.filter = query.filter;
        this.sort = query.comparators;
    }

    public QueryChangesMethodCall(String sinceQueryState) {
        this.sinceQueryState = sinceQueryState;
    }

}
