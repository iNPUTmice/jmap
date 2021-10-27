/*
 * Copyright 2019 Daniel Gultsch
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

package rs.ltt.jmap.client.session;

import static rs.ltt.jmap.client.Services.GSON;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.*;
import java.util.concurrent.Executors;
import okhttp3.HttpUrl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSessionCache implements SessionCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSessionCache.class);

    private static final ListeningExecutorService EXECUTOR_SERVICE =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    private final File directory;

    public FileSessionCache() {
        this.directory = null;
    }

    public FileSessionCache(@NonNull File directory) {
        this.directory = directory;
        LOGGER.debug("Initialize cache in {}", directory.getAbsolutePath());
    }

    @Override
    public void store(String username, HttpUrl sessionResource, Session session) {
        EXECUTOR_SERVICE.execute(
                () -> {
                    final File file = getFile(getFilename(username, sessionResource));
                    try {
                        final FileWriter fileWriter = new FileWriter(file);
                        GSON.toJson(session, fileWriter);
                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to cache session in {}", file.getAbsolutePath());
                    }
                });
    }

    @Override
    public ListenableFuture<Session> load(String username, HttpUrl sessionResource) {
        return EXECUTOR_SERVICE.submit(() -> loadFromFile(username, sessionResource));
    }

    private Session loadFromFile(final String username, final HttpUrl sessionResource) {
        final File file = getFile(getFilename(username, sessionResource));
        try {
            final Session session = GSON.fromJson(new FileReader(file), Session.class);
            LOGGER.debug("Restored session from {}", file.getAbsolutePath());
            return session;
        } catch (final FileNotFoundException e) {
            LOGGER.debug("Unable to restore session. {} not found", file.getAbsolutePath());
            return null;
        } catch (final Exception e) {
            LOGGER.warn("Unable to restore session", e);
            return null;
        }
    }

    private File getFile(final String filename) {
        if (directory == null) {
            return new File(filename);
        } else {
            return new File(directory, filename);
        }
    }

    private static String getFilename(String username, HttpUrl sessionResource) {
        final String name =
                username + ':' + (sessionResource == null ? '\00' : sessionResource.toString());
        return "session-cache-" + Hashing.sha256().hashString(name, Charsets.UTF_8).toString();
    }
}
