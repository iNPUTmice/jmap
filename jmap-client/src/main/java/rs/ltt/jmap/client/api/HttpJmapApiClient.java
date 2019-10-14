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


import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.http.BasicAuthHttpAuthentication;
import rs.ltt.jmap.client.http.HttpAuthentication;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HttpJmapApiClient extends AbstractJmapApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpJmapApiClient.class);

    private static MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();

    private final URL apiUrl;
    private final HttpAuthentication httpAuthentication;

    public HttpJmapApiClient(final URL apiUrl, String username, String password) {
        this(apiUrl, new BasicAuthHttpAuthentication(username, password));
    }

    public HttpJmapApiClient(final URL apiUrl, final HttpAuthentication httpAuthentication) {
        this.apiUrl = apiUrl;
        this.httpAuthentication = httpAuthentication;
    }

    @Override
    void onSessionStateRetrieved(final String sessionState) {
        LOGGER.debug("Notified of session state='{}'", sessionState);
    }

    @Override
    InputStream send(String out) throws IOException, JmapApiException {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(apiUrl);
        this.httpAuthentication.authenticate(requestBuilder);
        requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, out));
        final Response response = OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute();
        final int code = response.code();
        if (code == 404) {
            throw new EndpointNotFoundException(String.format("API URL(%s) not found", apiUrl));
        }
        if (code == 401) {
            throw new UnauthorizedException(String.format("API URL(%s) was unauthorized", apiUrl));
        }

        //TODO: code 500+ should probably just throw internal server error exception
        final ResponseBody body = response.body();
        if (body == null) {
            throw new IllegalStateException("response body was empty");
        }
        return body.byteStream();
    }
}
