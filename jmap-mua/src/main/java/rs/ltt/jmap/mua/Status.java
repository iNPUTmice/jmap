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

package rs.ltt.jmap.mua;

import com.google.common.util.concurrent.ListenableFuture;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.mua.cache.AbstractUpdate;

public enum Status {
    HAS_MORE,
    UPDATED,
    UNCHANGED;

    public static <T extends AbstractIdentifiableEntity> Status of(AbstractUpdate<T> update) {
        return update.isHasMore() ? HAS_MORE : of(update.hasChanges());
    }

    public static Status of(final boolean hasChanges) {
        return hasChanges ? UPDATED : UNCHANGED;
    }

    public static boolean unchanged(ListenableFuture<Status> statusListenableFuture) {
        try {
            return statusListenableFuture.get() == UNCHANGED;
        } catch (Exception e) {
            // exception also means no changes
            return true;
        }
    }
}
