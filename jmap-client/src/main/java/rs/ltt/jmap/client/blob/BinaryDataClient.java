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

package rs.ltt.jmap.client.blob;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.http.HttpAuthentication;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinaryDataClient {

    private static final String HTTP_HEADER_RANGE = "Range";
    private static final String HTTP_HEADER_CONTENT_RANGE = "Content-Range";
    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("(^[a-zA-Z][\\w]*)\\s+(\\d+)\\s?-\\s?(\\d+)?\\s?/?\\s?(\\d+|\\*)?");

    private final HttpAuthentication httpAuthentication;

    public BinaryDataClient(final HttpAuthentication httpAuthentication) {
        this.httpAuthentication = httpAuthentication;
    }

    public ListenableFuture<Download> download(final HttpUrl httpUrl, final long rangeStart) {
        final SettableFuture<Download> settableFuture = SettableFuture.create();
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(httpUrl);
        if (rangeStart > 0) {
            requestBuilder.header(HTTP_HEADER_RANGE, String.format("bytes=%d-", rangeStart));
        }
        this.httpAuthentication.authenticate(requestBuilder);
        final Call call = Services.OK_HTTP_CLIENT.newCall(requestBuilder.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                settableFuture.setException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                settableFuture.setFuture(onDownloadResponse(call, response, rangeStart));
            }
        });
        return settableFuture;
    }

    private ListenableFuture<Download> onDownloadResponse(@NotNull Call call, @NotNull Response response, final long rangeStart) {
        final ResponseBody body = response.body();
        if (body == null) {
            return Futures.immediateFailedFuture(new IllegalStateException("response body was empty"));
        }
        if (response.isSuccessful()) {
            final String contentLengthHeader = response.header(HTTP_HEADER_CONTENT_LENGTH);
            final ContentRange contentRange;
            try {
                contentRange = ContentRange.of(response.header(HTTP_HEADER_CONTENT_RANGE));
            } catch (final IllegalArgumentException e) {
                return Futures.immediateFailedFuture(e);
            }
            final Long contentLength = contentLengthHeader == null ? null : Longs.tryParse(contentLengthHeader);
            final boolean resumed = rangeStart > 0 && contentRange != null;
            final Download download;
            if (resumed) {
                if (rangeStart != contentRange.getStart()) {
                    return Futures.immediateFailedFuture(new IllegalStateException());
                }
                download = new Download(
                        call,
                        true,
                        contentRange.getEnd(),
                        body.byteStream()
                );
            } else {
                download = new Download(
                        call,
                        false,
                        contentLength == null ? 0 : contentLength,
                        body.byteStream()
                );
            }
            return Futures.immediateFuture(download);
        }
        final ProblemDetails details;
        try {
            details = Services.GSON.fromJson(new InputStreamReader(body.byteStream()), ProblemDetails.class);
        } catch (final Exception e) {
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFailedFuture(new BlobTransferException(response.code(), details));
    }

    public static class ContentRange {
        private final String unit;
        private final long start;
        private final long end;
        private final long contentLength;

        private ContentRange(String unit, Long start, Long end, Long contentLength) {
            this.unit = unit;
            this.start = start == null ? 0 : start;
            this.end = end == null ? 0 : end;
            this.contentLength = contentLength == null ? 0 : contentLength;
        }

        public static ContentRange of(final String header) {
            if (header == null) {
                return null;
            }
            final Matcher matcher = CONTENT_RANGE_PATTERN.matcher(header);
            if (matcher.matches()) {
                return new ContentRange(
                        matcher.group(1),
                        Longs.tryParse(matcher.group(2)),
                        Longs.tryParse(matcher.group(3)),
                        Longs.tryParse(matcher.group(4))
                );
            }
            throw new IllegalArgumentException(String.format("Invalid content range %s", header));
        }

        public String getUnit() {
            return unit;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getContentLength() {
            return contentLength;
        }
    }

}