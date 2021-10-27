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
 * A cache implementation will throw that for errors that can be recovered from automatically like
 * state mismatched states. (When attempting to update from state a to state c but the state in the
 * cache is b or d.) This usually means that the cache write attempt is a result for a request that
 * got delayed or duplicated in flight or something.
 *
 * <p>MUA will pass this error on to the higher ups but it is probably save to ignore as a request
 * with a correct update is probably already in flight or can easily be made.
 */
public class CacheConflictException extends IllegalStateException {
    public CacheConflictException(String message) {
        super(message);
    }
}
