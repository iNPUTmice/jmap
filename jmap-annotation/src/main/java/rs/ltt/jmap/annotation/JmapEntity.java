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

package rs.ltt.jmap.annotation;

import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.entity.filter.FilterCondition;

public @interface JmapEntity {

    Class<? extends FilterCondition<? extends AbstractIdentifiableEntity>> filterCondition() default NoFilterCondition.class;
    String name() default "";

    abstract class NoFilterCondition implements FilterCondition<AbstractIdentifiableEntity> {

        private NoFilterCondition() {
            throw new AssertionError("Do not try to instantiate me");
        }

        @Override
        public int compareTo(Filter<AbstractIdentifiableEntity> noneFilter) {
            return 0;
        }

        @Override
        public String toQueryString() {
            return null;
        }
    }
}
