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

package rs.ltt.jmap.client.api;

import rs.ltt.jmap.common.Request;

public class MethodResponseNotFoundException extends JmapApiException {

    MethodResponseNotFoundException(Request.Invocation invocation) {
        super(
                String.format(
                        "MethodResponse for invocation of %s with id=%s not found in server"
                                + " response",
                        invocation.getMethodCall().getClass().getName(), invocation.getId()));
    }
}
