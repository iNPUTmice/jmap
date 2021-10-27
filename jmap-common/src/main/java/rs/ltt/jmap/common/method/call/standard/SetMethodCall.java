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

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.AbstractIdentifiableEntity;
import rs.ltt.jmap.common.method.MethodCall;

@Getter
public abstract class SetMethodCall<T extends AbstractIdentifiableEntity> implements MethodCall {

    @NonNull private String accountId;

    private String ifInState;

    private Map<String, T> create;

    private Map<String, Map<String, Object>> update;

    private String[] destroy;

    @SerializedName("#destroy")
    private Request.Invocation.ResultReference destroyReference;

    public SetMethodCall(
            @NonNull String accountId,
            String ifInState,
            Map<String, T> create,
            Map<String, Map<String, Object>> update,
            String[] destroy,
            Request.Invocation.ResultReference destroyReference) {
        Preconditions.checkArgument(
                destroy == null || destroyReference == null,
                "Can't set both 'destroy' and 'destroyReference'");
        this.accountId = accountId;
        this.ifInState = ifInState;
        this.create = create;
        this.update = update;
        this.destroy = destroy;
        this.destroyReference = destroyReference;
    }
}
