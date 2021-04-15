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

package rs.ltt.jmap.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.client.api.UserAgentInterceptor;
import rs.ltt.jmap.gson.JmapAdapters;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class Services {

    public static final OkHttpClient OK_HTTP_CLIENT;
    public static final OkHttpClient OK_HTTP_CLIENT_LOGGING;
    public static final Gson GSON;
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    static {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final Logger OK_HTTP_LOGGER = LoggerFactory.getLogger(OkHttpClient.class);
        builder.addInterceptor(new UserAgentInterceptor());
        OK_HTTP_CLIENT = builder.build();
        if (OK_HTTP_LOGGER.isInfoEnabled()) {
            final OkHttpClient.Builder loggingBuilder = OK_HTTP_CLIENT.newBuilder();
            final HttpLoggingInterceptor loggingInterceptor;
            if (OK_HTTP_LOGGER.isDebugEnabled()) {
                loggingInterceptor = new HttpLoggingInterceptor(OK_HTTP_LOGGER::debug);
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            } else {
                loggingInterceptor = new HttpLoggingInterceptor(OK_HTTP_LOGGER::info);
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            }
            loggingBuilder.addInterceptor(loggingInterceptor);
            OK_HTTP_CLIENT_LOGGING = loggingBuilder.build();
        } else {
            OK_HTTP_CLIENT_LOGGING = OK_HTTP_CLIENT;
        }
        final GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    private Services() {
        throw new IllegalStateException("Do not instantiate this class");
    }


}
