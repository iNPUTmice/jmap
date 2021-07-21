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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUpload implements Uploadable, Closeable {

    private final InputStream inputStream;
    private final long contentLength;
    private final MediaType mediaType;

    private FileUpload(final Path path) throws IOException {
        this.inputStream = Files.newInputStream(path);
        this.contentLength = Files.size(path);
        this.mediaType = MediaType.parse(Files.probeContentType(path));

    }

    public static FileUpload of(final Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return new FileUpload(path);
        } else {
            throw new IOException(String.format("%s is not a regular file", path));
        }
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

    @Override
    public void close() throws IOException {
        this.inputStream.close();
    }
}
