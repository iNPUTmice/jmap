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

package rs.ltt.jmap.client.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodResponse;

// TODO test me
public class ResponseAnalyzer {

    private final ImmutableMap<String, MethodResponses> methodResponsesMap;

    private ResponseAnalyzer(Map<String, MethodResponses> map) {
        this.methodResponsesMap = ImmutableMap.copyOf(map);
    }

    public static ResponseAnalyzer analyse(Response response) {
        final ListMultimap<String, MethodResponse> preMap = ArrayListMultimap.create();
        for (Response.Invocation invocation : response.getMethodResponses()) {
            preMap.put(invocation.getId(), invocation.getMethodResponse());
        }
        final Map<String, MethodResponses> actualMap = new HashMap<>();
        for (final String id : preMap.keySet()) {
            final List<MethodResponse> methodResponseList = preMap.get(id);
            final MethodResponses methodResponses;
            if (methodResponseList.size() == 0) {
                throw new AssertionError("Method response list can not be empty");
            } else if (methodResponseList.size() == 1) {
                methodResponses = new MethodResponses(methodResponseList.get(0));
            } else {
                methodResponses =
                        new MethodResponses(
                                methodResponseList.get(0),
                                methodResponseList
                                        .subList(1, methodResponseList.size())
                                        .toArray(new MethodResponse[0]));
            }
            actualMap.put(id, methodResponses);
        }
        return new ResponseAnalyzer(actualMap);
    }

    public MethodResponses find(Request.Invocation invocation) {
        return methodResponsesMap.get(invocation.getId());
    }
}
