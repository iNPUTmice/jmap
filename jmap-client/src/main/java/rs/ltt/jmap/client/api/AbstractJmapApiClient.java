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

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rs.ltt.jmap.gson.JmapAdapters;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.client.util.ResponseAnalyzer;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public abstract class AbstractJmapApiClient implements JmapApiClient {

    private final GsonBuilder gsonBuilder;

    AbstractJmapApiClient() {
        gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
    }

    abstract void onSessionStateRetrieved(String sessionState);

    abstract InputStream send(String out) throws IOException, JmapApiException;

    @Override
    public void execute(final JmapRequest jmapRequest) {
        try {
            final Gson gson = gsonBuilder.create();
            final InputStream inputStream = send(gson.toJson(jmapRequest.getRequest()));
            final GenericResponse genericResponse = gson.fromJson(new InputStreamReader(inputStream), GenericResponse.class);
            if (genericResponse instanceof ErrorResponse) {
                jmapRequest.setException(new ErrorResponseException((ErrorResponse) genericResponse));
            } else if (genericResponse instanceof Response) {
                final Response response = (Response) genericResponse;
                final ResponseAnalyzer responseAnalyzer = ResponseAnalyzer.analyse(response);
                final Map<Request.Invocation, SettableFuture<MethodResponses>> map = jmapRequest.getInvocationFutureImmutableMap();

                // Notify about potentially updated session state *before* setting the response futures. This way we'll
                // make sure that additional requests guarded by a wait on one of the response futures will trigger
                // re-fetching the session resource.
                this.onSessionStateRetrieved(response.getSessionState());

                for(Map.Entry<Request.Invocation, SettableFuture<MethodResponses>> entry : map.entrySet()) {
                    final Request.Invocation invocation = entry.getKey();
                    final SettableFuture<MethodResponses> future = entry.getValue();
                    final MethodResponses methodResponses = responseAnalyzer.find(invocation);
                    if (methodResponses == null) {
                        future.setException(new MethodResponseNotFoundException(invocation));
                        continue;
                    }
                    final MethodResponse main = methodResponses.getMain();
                    if (main instanceof MethodErrorResponse) {
                        future.setException(new MethodErrorResponseException((MethodErrorResponse) main, methodResponses.getAdditional()));
                    } else {
                        future.set(methodResponses);
                    }
                }
            }
        } catch (Exception e) {
            jmapRequest.setException(e);
        }
    }
}
