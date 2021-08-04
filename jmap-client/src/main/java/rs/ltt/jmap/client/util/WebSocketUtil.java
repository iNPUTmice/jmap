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

package rs.ltt.jmap.client.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import okhttp3.HttpUrl;

import java.util.Locale;

public final class WebSocketUtil {

    private static final ImmutableBiMap<String, String> SCHEME_MAP = new ImmutableBiMap.Builder<String, String>()
            .put("ws", "http")
            .put("wss", "https")
            .build();

    private WebSocketUtil() {

    }

    public static HttpUrl normalizeUrl(final HttpUrl base, final String url) {
        final int schemeEndIndex = url.indexOf(":");
        if (schemeEndIndex == -1) {
            final HttpUrl.Builder builder = base.newBuilder(url);
            Preconditions.checkState(
                    builder != null,
                    String.format("Unable to assemble final WebSocket URL from base=%s and url=%s", base, url)
            );
            return builder.build();
        }
        final String scheme = url.substring(0, schemeEndIndex).toLowerCase(Locale.ENGLISH);
        if (SCHEME_MAP.containsKey(scheme)) {
            final String normalizedScheme = SCHEME_MAP.get(scheme);
            final String authority = url.substring(scheme.length() + 1);
            if (authority.startsWith("//")) {
                return HttpUrl.get(String.format("%s:%s", normalizedScheme, authority));
            } else {
                final HttpUrl.Builder builder = base.newBuilder(authority);
                Preconditions.checkState(
                        builder != null,
                        String.format("Unable to assemble final WebSocket URL from base=%s and url=%s", base, url)
                );
                builder.scheme(normalizedScheme);
                return builder.build();
            }
        }
        return HttpUrl.get(url);
    }
}
