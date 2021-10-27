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

package rs.ltt.jmap.common.method.response.core;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.PushSubscription;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.method.MethodResponse;

@JmapMethod("PushSubscription/set")
@Getter
@Builder
public class SetPushSubscriptionMethodResponse implements MethodResponse {

    private String oldState;
    private String newState;

    @Singular("created")
    private Map<String, PushSubscription> created;

    @Singular("updated")
    private Map<String, PushSubscription> updated;

    private String[] destroyed;
    private Map<String, SetError> notCreated;

    @Singular("notUpdated")
    private Map<String, SetError> notUpdated;

    private Map<String, SetError> notDestroyed;
}
