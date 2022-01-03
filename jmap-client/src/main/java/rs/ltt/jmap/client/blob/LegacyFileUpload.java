/*
 * Copyright 2022 Daniel Gultsch
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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class LegacyFileUpload implements Uploadable, Closeable {

    private final FileInputStream inputStream;
    private final long contentLength;
    private final MediaType mediaType;

    private LegacyFileUpload(final File file, final MediaType mediaType)
            throws FileNotFoundException {
        this.inputStream = new FileInputStream(file);
        this.contentLength = file.length();
        this.mediaType = mediaType;
    }

    public static LegacyFileUpload of(final File file, final MediaType mediaType)
            throws IOException {
        if (file.isFile()) {
            return new LegacyFileUpload(file, mediaType);
        } else {
            throw new IOException(
                    String.format("%s is not a regular file", file.getAbsoluteFile()));
        }
    }

    @Override
    public FileInputStream getInputStream() {
        if (this.inputStream.closed) {
            throw new IllegalStateException(
                    "FileInputStream has already been closed. Are you using a network"
                            + " interceptor/logger?");
        }
        return inputStream;
    }

    @Override
    public MediaType getMediaType() {
        return this.mediaType;
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }

    @Override
    public void close() throws IOException {
        this.inputStream.close();
    }

    private static class FileInputStream extends java.io.FileInputStream {

        private boolean closed = false;

        public FileInputStream(@NotNull File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            super.close();
        }
    }
}
