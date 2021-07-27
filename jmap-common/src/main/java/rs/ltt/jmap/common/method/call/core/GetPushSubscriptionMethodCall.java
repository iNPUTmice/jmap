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

package rs.ltt.jmap.common.method.call.core;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.NonNull;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.PushSubscription;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("PushSubscription/get")
public class GetPushSubscriptionMethodCall implements MethodCall {

    private String[] ids;

    protected String[] properties;

    @SerializedName("#ids")
    private Request.Invocation.ResultReference idsReference;

    @Builder
    public GetPushSubscriptionMethodCall(String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
        Preconditions.checkArgument(ids == null || idsReference == null, "Can't set both 'ids' and 'idsReference'");
        this.ids = ids;
        this.properties = properties;
        this.idsReference = idsReference;
    }
}
