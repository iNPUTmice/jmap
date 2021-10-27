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

package rs.ltt.jmap.common;

import rs.ltt.jmap.common.method.MethodResponse;

public class Response implements GenericResponse {

    private final Invocation[] methodResponses;
    private final String sessionState;

    public Response(final Invocation[] methodResponses, final String sessionState) {
        this.methodResponses = methodResponses;
        this.sessionState = sessionState;
    }

    public Invocation[] getMethodResponses() {
        return methodResponses;
    }

    public String getSessionState() {
        return sessionState;
    }

    public static class Invocation {
        private MethodResponse methodResponse;
        private String id;

        public Invocation() {}

        public Invocation(MethodResponse methodResponse, String id) {
            this.methodResponse = methodResponse;
            this.id = id;
        }

        public MethodResponse getMethodResponse() {
            return methodResponse;
        }

        public String getId() {
            return id;
        }
    }
}
