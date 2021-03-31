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

import com.google.common.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.MethodResponses;
import rs.ltt.jmap.client.util.ResponseAnalyzer;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public abstract class AbstractJmapApiClient implements JmapApiClient {

    private final Gson gson;

    AbstractJmapApiClient() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        gson = gsonBuilder.create();
    }

    //TODO this execute() method seems to be specific to Http - we might want to move this to HttpJmapApiClient
    //and leave processResponse(request, GenericResponse) in here
    @Override
    public void execute(final JmapRequest jmapRequest) {
        final String json;
        try {
            json = gson.toJson(jmapRequest.getRequest());
        } catch (final Throwable throwable) {
            jmapRequest.setException(throwable);
            return;
        }
        Futures.addCallback(send(json), new FutureCallback<InputStream>() {
            @Override
            public void onSuccess(final InputStream inputStream) {
                try {
                    processResponse(jmapRequest, inputStream);
                } catch (final RuntimeException e) {
                    jmapRequest.setException(e);
                }
            }

            @Override
            public void onFailure(@Nonnull Throwable throwable) {
                jmapRequest.setException(throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    protected void processResponse(final JmapRequest jmapRequest, final InputStream inputStream) {
        final GenericResponse genericResponse = gson.fromJson(new InputStreamReader(inputStream), GenericResponse.class);
        processResponse(jmapRequest, genericResponse);
    }

    protected void processResponse(final JmapRequest jmapRequest, final GenericResponse genericResponse) {
        if (genericResponse instanceof ErrorResponse) {
            jmapRequest.setException(new ErrorResponseException((ErrorResponse) genericResponse));
        } else if (genericResponse instanceof Response) {
            final Response response = (Response) genericResponse;
            final ResponseAnalyzer responseAnalyzer = ResponseAnalyzer.analyse(response);
            final Map<Request.Invocation, SettableFuture<MethodResponses>> map = jmapRequest.getInvocationFutureImmutableMap();

            // Notify about potentially updated session state *before* setting the response futures. This way we'll
            // make sure that additional requests guarded by a wait on one of the response futures will trigger
            // re-fetching the session resource.
            onSessionStateRetrieved(response.getSessionState());

            for (Map.Entry<Request.Invocation, SettableFuture<MethodResponses>> entry : map.entrySet()) {
                final Request.Invocation invocation = entry.getKey();
                final SettableFuture<MethodResponses> future = entry.getValue();
                final MethodResponses methodResponses = responseAnalyzer.find(invocation);
                if (methodResponses == null) {
                    future.setException(new MethodResponseNotFoundException(invocation));
                    continue;
                }
                final MethodResponse main = methodResponses.getMain();
                if (main instanceof MethodErrorResponse) {
                    future.setException(new MethodErrorResponseException((MethodErrorResponse) main,
                            methodResponses.getAdditional(),
                            invocation.getMethodCall())
                    );
                } else {
                    future.set(methodResponses);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    String.format("Unable to process response of type %s", genericResponse.getClass().getName())
            );
        }
    }

    abstract void onSessionStateRetrieved(String sessionState);

    abstract ListenableFuture<InputStream> send(String out);
}
