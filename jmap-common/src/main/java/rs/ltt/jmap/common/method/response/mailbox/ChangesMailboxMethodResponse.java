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

package rs.ltt.jmap.common.method.response.mailbox;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.method.response.standard.ChangesMethodResponse;

@JmapMethod("Mailbox/changes")
public class ChangesMailboxMethodResponse extends ChangesMethodResponse<Mailbox> {

    private String[] updatedProperties;

    @Builder
    public ChangesMailboxMethodResponse(
            String accountId,
            String oldState,
            String newState,
            boolean hasMoreChanges,
            String[] created,
            String[] updated,
            String[] destroyed,
            String[] updatedProperties) {
        super(accountId, oldState, newState, hasMoreChanges, created, updated, destroyed);
        this.updatedProperties = updatedProperties;
    }

    public String[] getUpdatedProperties() {
        return updatedProperties;
    }
}
