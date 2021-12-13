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

import com.google.common.net.MediaType;
import java.io.*;

public class OutputStreamUpload implements Uploadable {

    private final MediaType mediaType;
    private final long contentLength;
    private final PipedInputStream inputStream;

    private OutputStreamUpload(MediaType mediaType, long contentLength) {
        this.mediaType = mediaType;
        this.contentLength = contentLength;
        this.inputStream = new PipedInputStream();
    }

    public static OutputStreamUpload of(final MediaType mediaType) {
        return new OutputStreamUpload(mediaType, -1);
    }

    public static OutputStreamUpload of(final MediaType mediaType, final long contentLength) {
        return new OutputStreamUpload(mediaType, contentLength);
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    public PipedOutputStream getOutputStream() throws IOException {
        return new PipedOutputStream(inputStream);
    }
}
