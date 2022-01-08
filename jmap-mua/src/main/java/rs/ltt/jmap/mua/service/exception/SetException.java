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

package rs.ltt.jmap.mua.service.exception;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import java.util.*;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.entity.SetErrorType;

public abstract class SetException extends Exception {

    private final Map<String, SetError> notCreated;
    private final Map<String, SetError> notUpdated;
    private final Map<String, SetError> notDestroyed;

    protected SetException(
            Map<String, SetError> notCreated,
            Map<String, SetError> notUpdated,
            Map<String, SetError> notDestroyed) {
        super(message(notCreated, notUpdated, notDestroyed));
        this.notCreated = notCreated;
        this.notUpdated = notUpdated;
        this.notDestroyed = notDestroyed;
    }

    private static String message(
            final Map<String, SetError> notCreated,
            final Map<String, SetError> notUpdated,
            final Map<String, SetError> notDestroyed) {
        final List<String> messages =
                Arrays.asList(
                        message("created", notCreated),
                        message("updated", notUpdated),
                        message("destroyed", notDestroyed));
        return Joiner.on(' ').join(Collections2.filter(messages, Objects::nonNull));
    }

    private static String message(final String action, final Map<String, SetError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        final Collection<SetErrorType> types =
                Collections2.transform(errors.values(), SetError::getType);
        return String.format("not %s: (%s)", action, Joiner.on(", ").join(types));
    }

    public Map<String, SetError> getNotCreated() {
        return notCreated;
    }

    public Map<String, SetError> getNotUpdated() {
        return notUpdated;
    }

    public Map<String, SetError> getNotDestroyed() {
        return notDestroyed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("notCreated", notCreated)
                .add("notUpdated", notUpdated)
                .add("notDestroyed", notDestroyed)
                .toString();
    }
}
