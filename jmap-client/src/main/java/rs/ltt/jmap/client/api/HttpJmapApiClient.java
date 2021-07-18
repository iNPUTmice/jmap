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


import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.JmapRequest;
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication;
import rs.ltt.jmap.client.http.HttpAuthentication;
import rs.ltt.jmap.client.session.Session;
import rs.ltt.jmap.common.GenericResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static rs.ltt.jmap.client.Services.GSON;
import static rs.ltt.jmap.client.Services.OK_HTTP_CLIENT_LOGGING;

public class HttpJmapApiClient extends AbstractJmapApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpJmapApiClient.class);

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");

    private final HttpUrl apiUrl;
    private final HttpAuthentication httpAuthentication;

    public HttpJmapApiClient(final HttpUrl apiUrl, String username, String password) {
        this(apiUrl, new BasicAuthHttpAuthentication(username, password), null);
    }

    public HttpJmapApiClient(final HttpUrl apiUrl, final HttpAuthentication httpAuthentication, @Nullable final SessionStateListener sessionStateListener) {
        super(sessionStateListener);
        this.apiUrl = Preconditions.checkNotNull(apiUrl, "This API URL must not be null");
        this.httpAuthentication = httpAuthentication;
    }

    public HttpJmapApiClient(final HttpUrl apiUrl, final HttpAuthentication httpAuthentication) {
        this(apiUrl, httpAuthentication, null);
    }

    private ListenableFuture<InputStream> send(final String out) {
        //TODO replace with SettableCallFuture?
        final SettableFuture<InputStream> settableInputStreamFuture = SettableFuture.create();
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(apiUrl);
        this.httpAuthentication.authenticate(requestBuilder);
        requestBuilder.post(RequestBody.create(out, MEDIA_TYPE_JSON));
        OK_HTTP_CLIENT_LOGGING.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                settableInputStreamFuture.setException(e);
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) {
                final int code = response.code();
                if (code == 404) {
                    settableInputStreamFuture.setException(
                            new EndpointNotFoundException(String.format("API URL(%s) not found", apiUrl))
                    );
                    return;
                }
                if (code == 401) {
                    settableInputStreamFuture.setException(
                            new UnauthorizedException(String.format("API URL(%s) was unauthorized", apiUrl))
                    );
                    return;
                }

                //TODO: code 500+ should probably just throw internal server error exception
                final ResponseBody body = response.body();
                if (body == null) {
                    settableInputStreamFuture.setException(
                            new IllegalStateException("response body was empty")
                    );
                    return;
                }
                settableInputStreamFuture.set(body.byteStream());
            }
        });
        return settableInputStreamFuture;
    }

    @Override
    public void execute(final JmapRequest jmapRequest) {
        final String json;
        try {
            json = GSON.toJson(jmapRequest.getRequest());
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
        final GenericResponse genericResponse = GSON.fromJson(new InputStreamReader(inputStream), GenericResponse.class);
        processResponse(jmapRequest, genericResponse);
    }

    @Override
    public boolean isValidFor(final Session session) {
        return this.apiUrl.equals(session.getApiUrl());
    }
}
