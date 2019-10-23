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

package rs.ltt.jmap.mua.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;

import java.util.Locale;

public class CreateUtil {

    public static Optional<ListenableFuture<MethodResponses>> mailbox(final JmapClient.MultiCall multiCall, final IdentifiableMailboxWithRole mailbox, final Role role) {
        if (mailbox != null) {
            Preconditions.checkArgument(mailbox.getRole() == role);
        }
        final Optional<ListenableFuture<MethodResponses>> mailboxCreateFutureOptional;
        if (mailbox == null) {
            return Optional.of(multiCall.call(new SetMailboxMethodCall(ImmutableMap.of(createId(role), MailboxUtil.create(role)))).getMethodResponses());
        } else {
            return Optional.absent();
        }
    }

    private static String createId(Role role) {
        return "mb-" + role.toString().toLowerCase(Locale.US);
    }

    public static String createIdReference(Role role) {
        return "#" + createId(role);
    }
}
