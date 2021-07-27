/*
 * Copyright 2021 Daniel Gultsch
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

package rs.ltt.jmap.mock.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.entity.PushMessage;
import rs.ltt.jmap.gson.JmapAdapters;

public final class Pusher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pusher.class);

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();

    private static final Gson GSON;

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    private Pusher() {

    }


    public static boolean push(final HttpUrl url, final PushMessage message) {
        LOGGER.info("push {} to {}", message.getClass().getSimpleName(), url);
        final Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(GSON.toJson(message), MEDIA_TYPE_JSON));
        try {
            final Response response = OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            LOGGER.info("Unable to push", e);
            return false;
        }
    }

}
