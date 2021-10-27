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

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RequestBodies {

    private static final int BLOCK_SIZE = 8196;

    public static RequestBody of(final Uploadable uploadable, final Progress progress) {
        return new RequestBody() {

            @Override
            public long contentLength() {
                return uploadable.getContentLength();
            }

            @Nullable
            @Override
            public MediaType contentType() {
                return convert(uploadable.getMediaType());
            }

            @Override
            public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
                long done = 0;
                try (final Source source = Okio.source(uploadable.getInputStream())) {
                    long read;
                    while ((read = source.read(bufferedSink.getBuffer(), BLOCK_SIZE)) != -1) {
                        done += read;
                        if (progress != null) {
                            bufferedSink.flush();
                            progress.onProgress(
                                    Progress.progress(done, uploadable.getContentLength()));
                        }
                    }
                }
                bufferedSink.flush();
            }
        };
    }

    private static MediaType convert(com.google.common.net.MediaType mediaType) {
        return MediaType.get(String.format("%s/%s", mediaType.type(), mediaType.subtype()));
    }
}
