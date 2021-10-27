/*
 * Copyright 2020 Daniel Gultsch
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

package rs.ltt.jmap.mua.cache.exception;

/**
 * A Cache implementation should throw this when it detects a problem it can not simply recover
 * from. For example seeing an update to an element it doesn't have stored or processing an
 * addQueryResult when the previous page is corrupt or out of sync. MUA will then mark the cache for
 * that object or for that query as invalid and overwrite it on the next request.
 *
 * <p>This exception should not be thrown lightly as rebuilding the cache is potentially expensive.
 */
public class CorruptCacheException extends IllegalStateException {

    public CorruptCacheException(String message) {
        super(message);
    }
}
