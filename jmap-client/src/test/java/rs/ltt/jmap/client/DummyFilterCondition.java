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

package rs.ltt.jmap.client;

import javax.annotation.Nonnull;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.filter.FilterCondition;

public class DummyFilterCondition implements FilterCondition<Dummy> {

    private Boolean isPlaceholder;

    public DummyFilterCondition(Boolean isPlaceholder) {
        this.isPlaceholder = isPlaceholder;
    }

    @Override
    public int compareTo(@Nonnull Filter<Dummy> dummyFilter) {
        return 0;
    }

    @Override
    public String toQueryString() {
        return null;
    }

    public Boolean getPlaceholder() {
        return isPlaceholder;
    }
}
