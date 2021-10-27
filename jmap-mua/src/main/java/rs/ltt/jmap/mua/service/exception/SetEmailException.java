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

import java.util.Map;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse;

public class SetEmailException extends SetException {

    private SetEmailException(
            Map<String, SetError> notCreated,
            Map<String, SetError> notUpdated,
            Map<String, SetError> notDestroyed) {
        super(notCreated, notUpdated, notDestroyed);
    }

    public static void throwIfFailed(SetEmailMethodResponse response) throws SetEmailException {
        Map<String, SetError> notCreated = response.getNotCreated();
        Map<String, SetError> notUpdated = response.getNotUpdated();
        Map<String, SetError> notDestroyed = response.getNotDestroyed();
        if ((notCreated != null && notCreated.size() > 0)
                || (notUpdated != null && notUpdated.size() > 0)
                || (notDestroyed != null && notDestroyed.size() > 0)) {
            throw new SetEmailException(notCreated, notUpdated, notDestroyed);
        }
    }
}
