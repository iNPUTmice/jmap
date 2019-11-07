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

package rs.ltt.jmap.common.method.call.standard;

import com.google.gson.annotations.SerializedName;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.method.MethodCall;

public abstract class GetMethodCall<T extends AbstractIdentifiableEntity> implements MethodCall {

    private String accountId;
    private String[] ids;
    protected String[] properties;

    @SerializedName("#ids")
    private Request.Invocation.ResultReference idsReference;

    public GetMethodCall(String accountId, Request.Invocation.ResultReference idsReference) {
        this.accountId = accountId;
        this.idsReference = idsReference;
    }


    public GetMethodCall(String accountId, String[] ids) {
        this.accountId = accountId;
        this.ids = ids;
    }

    public GetMethodCall(String accountId) {
        this.accountId = accountId;
    }

}
