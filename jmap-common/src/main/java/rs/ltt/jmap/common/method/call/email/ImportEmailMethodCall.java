/*
 * Copyright 2020 cketti
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

package rs.ltt.jmap.common.method.call.email;

import lombok.Builder;
import lombok.NonNull;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.EmailImport;
import rs.ltt.jmap.common.method.MethodCall;

import java.util.Map;

@JmapMethod("Email/import")
@Builder
public class ImportEmailMethodCall implements MethodCall {

    @NonNull
    private String accountId;

    private String ifInState;

    @NonNull
    private Map<String, EmailImport> emails;

}
