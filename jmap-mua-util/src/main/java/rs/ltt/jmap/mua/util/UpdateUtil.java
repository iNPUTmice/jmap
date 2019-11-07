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

import com.google.common.util.concurrent.ListenableFuture;
import rs.ltt.jmap.client.JmapClient;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.email.ChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.identity.ChangesIdentityMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;

public class UpdateUtil {

    public static MethodResponsesFuture emails(JmapClient.MultiCall multiCall, String accountId, String state) {
        final JmapRequest.Call changesCallInfo = multiCall.call(new ChangesEmailMethodCall(state));
        final ListenableFuture<MethodResponses> changes = changesCallInfo.getMethodResponses();
        final ListenableFuture<MethodResponses> created = multiCall.call(new GetEmailMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.CREATED),
                true
        )).getMethodResponses();
        final ListenableFuture<MethodResponses> updated = multiCall.call(new GetEmailMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.UPDATED),
                true
        )).getMethodResponses();

        return new MethodResponsesFuture(changes, created, updated);
    }

    public static MethodResponsesFuture identities(JmapClient.MultiCall multiCall, String accountId, String state) {
        final JmapRequest.Call changesCallInfo = multiCall.call(new ChangesIdentityMethodCall(state));
        final ListenableFuture<MethodResponses> changes = changesCallInfo.getMethodResponses();
        final ListenableFuture<MethodResponses> created = multiCall.call(new GetIdentityMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.CREATED)
        )).getMethodResponses();
        final ListenableFuture<MethodResponses> updated = multiCall.call(new GetIdentityMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.UPDATED)
        )).getMethodResponses();

        return new MethodResponsesFuture(changes, created, updated);
    }

    public static MethodResponsesFuture mailboxes(JmapClient.MultiCall multiCall, String accountId, String state) {
        final JmapRequest.Call changesCallInfo = multiCall.call(new ChangesMailboxMethodCall(state));
        final ListenableFuture<MethodResponses> changes = changesCallInfo.getMethodResponses();
        final ListenableFuture<MethodResponses> created = multiCall.call(new GetMailboxMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.CREATED)
        )).getMethodResponses();
        final ListenableFuture<MethodResponses> updated = multiCall.call(new GetMailboxMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.UPDATED),
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.UPDATED_PROPERTIES)
        )).getMethodResponses();

        return new MethodResponsesFuture(changes, created, updated);
    }

    public static MethodResponsesFuture threads(JmapClient.MultiCall multiCall, String accountId, String state) {
        final JmapRequest.Call changesCallInfo = multiCall.call(new ChangesThreadMethodCall(state));
        final ListenableFuture<MethodResponses> changes = changesCallInfo.getMethodResponses();
        final ListenableFuture<MethodResponses> created = multiCall.call(new GetThreadMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.CREATED)
        )).getMethodResponses();
        final ListenableFuture<MethodResponses> updated = multiCall.call(new GetThreadMethodCall(
                accountId,
                changesCallInfo.createResultReference(Request.Invocation.ResultReference.Path.UPDATED)
        )).getMethodResponses();

        return new MethodResponsesFuture(changes, created, updated);
    }

    public static class MethodResponsesFuture {
        public final ListenableFuture<MethodResponses> changes;
        public final ListenableFuture<MethodResponses> created;
        public final ListenableFuture<MethodResponses> updated;

        private MethodResponsesFuture(ListenableFuture<MethodResponses> changes, ListenableFuture<MethodResponses> created, ListenableFuture<MethodResponses> updated) {
            this.changes = changes;
            this.created = created;
            this.updated = updated;
        }
    }

}
