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

package rs.ltt.jmap.common.method.call.mailbox;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("Mailbox/get")
public class GetMailboxMethodCall extends GetMethodCall<Mailbox> {

    @SerializedName("#properties")
    private Request.Invocation.ResultReference propertiesReference;

    @Builder
    public GetMailboxMethodCall(
            String accountId,
            String[] ids,
            String[] properties,
            Request.Invocation.ResultReference idsReference,
            Request.Invocation.ResultReference propertiesReference) {
        super(accountId, ids, properties, idsReference);
        Preconditions.checkArgument(
                properties == null || propertiesReference == null,
                "Can't set both 'properties' and 'propertiesReference'");
        this.propertiesReference = propertiesReference;
    }
}
