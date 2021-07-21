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

package rs.ltt.jmap.mua;

import com.google.common.collect.Iterables;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rs.ltt.jmap.client.blob.FileUpload;
import rs.ltt.jmap.client.blob.Upload;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class FileUploadTest {

    @TempDir
    Path tempDir;

    //@Test
    public void createAndUploadTextFile() throws IOException, ExecutionException, InterruptedException {
        final Path textFileLocation = tempDir.resolve("test.txt");
        Files.write(textFileLocation, "hello world".getBytes(StandardCharsets.UTF_8));
        System.out.println(textFileLocation);

        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);

        final Mua mua = Mua.builder()
                .cache(new InMemoryCache())
                .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                .username(mailServer.getUsername())
                .password(JmapDispatcher.PASSWORD)
                .accountId(mailServer.getAccountId())
                .build();

        try(final FileUpload fileUpload = FileUpload.of(textFileLocation)) {
            final Upload upload = mua.upload(fileUpload, null).get();
        }
    }

}
