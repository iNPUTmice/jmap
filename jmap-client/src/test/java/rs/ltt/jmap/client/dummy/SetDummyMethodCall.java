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

package rs.ltt.jmap.client.dummy;

import java.util.Map;
import rs.ltt.jmap.client.Dummy;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.standard.SetMethodCall;

public class SetDummyMethodCall extends SetMethodCall<Dummy> {

    public SetDummyMethodCall(
            String accountId,
            String ifInState,
            Map<String, Dummy> create,
            Map<String, Map<String, Object>> update,
            String[] destroy,
            Request.Invocation.ResultReference destroyReference) {
        super(accountId, ifInState, create, update, destroy, destroyReference);
    }
}
