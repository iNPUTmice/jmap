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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import okhttp3.HttpUrl;

public class InMemorySessionCache implements SessionCache {

    private final HashMap<String, Session> cache = new HashMap<>();

    @Override
    public void store(final String username, final HttpUrl sessionResource, Session session) {
        synchronized (this.cache) {
            cache.put(getKey(username, sessionResource), session);
        }
    }

    @Override
    public ListenableFuture<Session> load(final String username, final HttpUrl sessionResource) {
        synchronized (this.cache) {
            return Futures.immediateFuture(cache.get(getKey(username, sessionResource)));
        }
    }

    private static String getKey(final String username, final HttpUrl sessionResource) {
        final String name =
                username + ':' + (sessionResource == null ? '\00' : sessionResource.toString());
        return Hashing.sha256().hashString(name, Charsets.UTF_8).toString();
    }
}
