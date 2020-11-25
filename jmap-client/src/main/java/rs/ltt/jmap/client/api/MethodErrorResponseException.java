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

import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.util.Mapper;

public class MethodErrorResponseException extends JmapApiException {

    private final MethodErrorResponse methodErrorResponse;
    private final MethodResponse[] additional;
    private final MethodCall methodCall;

    MethodErrorResponseException(MethodErrorResponse methodErrorResponse, MethodResponse[] additional, MethodCall methodCall) {
        super(methodErrorResponse.getType() + ((additional != null && additional.length > 0) ? " + " + additional.length : "") + " in response to " + Mapper.METHOD_CALLS.inverse().get(methodCall.getClass()));
        this.methodErrorResponse = methodErrorResponse;
        this.additional = additional;
        this.methodCall = methodCall;
    }

    public MethodErrorResponse getMethodErrorResponse() {
        return methodErrorResponse;
    }

    public MethodResponse[] getAdditional() {
        return additional;
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    public static boolean matches(Throwable throwable, Class<? extends MethodErrorResponse> methodError) {
        if (throwable instanceof MethodErrorResponseException) {
            final MethodErrorResponseException methodErrorResponseException = (MethodErrorResponseException) throwable;
            return methodError.isInstance(methodErrorResponseException.getMethodErrorResponse());
        }
        return false;
    }
}
