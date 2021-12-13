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

import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.blob.Download;
import rs.ltt.jmap.common.entity.Downloadable;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.cache.InMemoryCache;

public class MuaDownloadTest {

    @Test
    public void downloadResource() throws ExecutionException, InterruptedException, IOException {
        final MockWebServer server = new MockWebServer();
        final MockMailServer mailServer = new MockMailServer(2);
        server.setDispatcher(mailServer);

        final Mua mua =
                Mua.builder()
                        .cache(new InMemoryCache())
                        .sessionResource(server.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .username(mailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mailServer.getAccountId())
                        .build();
        final Downloadable downloadable =
                EmailBodyPart.builder().blobId("d3d9da72-eff2-4e7c-b682-25efa6972dee").build();
        final Download download = mua.download(downloadable).get();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ByteStreams.copy(download.getInputStream(), byteArrayOutputStream);
        Assertions.assertEquals("Hello World!\n", byteArrayOutputStream.toString());
    }
}
