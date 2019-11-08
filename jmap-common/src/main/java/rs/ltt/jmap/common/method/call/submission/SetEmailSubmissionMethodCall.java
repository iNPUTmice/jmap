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

package rs.ltt.jmap.common.method.call.submission;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.EmailSubmission;
import rs.ltt.jmap.common.method.call.standard.SetMethodCall;

import java.util.List;
import java.util.Map;

@JmapMethod("EmailSubmission/set")
public class SetEmailSubmissionMethodCall extends SetMethodCall<EmailSubmission> {

    private Map<String, Map<String, Object>> onSuccessUpdateEmail;
    private List<String> onSuccessDestroyEmail;

    public SetEmailSubmissionMethodCall(String accountId, String ifInState, Map<String, EmailSubmission> create, Map<String, Map<String, Object>> update, String[] destroy, Map<String, Map<String, Object>> onSuccessUpdateEmail, List<String> onSuccessDestroyEmail) {
        super(accountId, ifInState, create, update, destroy);
        this.onSuccessUpdateEmail = onSuccessUpdateEmail;
        this.onSuccessDestroyEmail = onSuccessDestroyEmail;
    }

    public SetEmailSubmissionMethodCall(String accountId, String ifInState, String[] destroy) {
        super(accountId, ifInState, destroy);
    }

    public SetEmailSubmissionMethodCall(String accountId, String ifInState, String[] destroy, List<String> onSuccessDestroyEmail) {
        super(accountId, ifInState, destroy);
        this.onSuccessDestroyEmail = onSuccessDestroyEmail;
    }

    public SetEmailSubmissionMethodCall(String accountId, String ifInState, Map<String, Map<String, Object>> update) {
        super(accountId, ifInState, update);
    }

    public SetEmailSubmissionMethodCall(String accountId, String ifInState, Map<String, Map<String, Object>> update, Map<String, Map<String, Object>> onSuccessUpdateEmail) {
        super(accountId, ifInState, update);
        this.onSuccessUpdateEmail = onSuccessUpdateEmail;
    }

    public SetEmailSubmissionMethodCall(String accountId, Map<String, EmailSubmission> create) {
        super(accountId, create);
    }

    public SetEmailSubmissionMethodCall(String accountId, Map<String, EmailSubmission> create, Map<String, Map<String, Object>> onSuccessUpdateEmail) {
        super(accountId, create);
        this.onSuccessUpdateEmail = onSuccessUpdateEmail;
    }
}
