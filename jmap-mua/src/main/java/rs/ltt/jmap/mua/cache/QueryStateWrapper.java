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

public class QueryStateWrapper {

    public final String queryState;
    public final boolean canCalculateChanges;
    public final UpTo upTo;
    public final ObjectsState objectsState;

    public QueryStateWrapper(
            String queryState, boolean canCalculateChanges, UpTo upTo, ObjectsState objectsState) {
        this.queryState = queryState;
        this.canCalculateChanges = canCalculateChanges;
        this.upTo = upTo;
        this.objectsState = objectsState;
    }

    public static class UpTo {
        public final String id;
        public final long position;

        public UpTo(final String id, final long position) {
            this.id = id;
            this.position = position;
        }
    }
}
