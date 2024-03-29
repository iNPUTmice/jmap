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

package rs.ltt.jmap.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.client.blob.BinaryDataClient;

public class ContentRangeTest {

    @Test
    public void unitStartEnd() {
        final BinaryDataClient.ContentRange contentRange =
                BinaryDataClient.ContentRange.of("bytes 10-20/*");
        Assertions.assertEquals(10, contentRange.getStart());
        Assertions.assertEquals(20, contentRange.getEnd());
        Assertions.assertEquals(0, contentRange.getContentLength());
    }

    @Test
    public void unitStartEndContentLength() {
        final BinaryDataClient.ContentRange contentRange =
                BinaryDataClient.ContentRange.of("bytes 10-20/20");
        Assertions.assertEquals(10, contentRange.getStart());
        Assertions.assertEquals(20, contentRange.getEnd());
        Assertions.assertEquals(20, contentRange.getContentLength());
    }

    @Test
    public void invalid() {
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> BinaryDataClient.ContentRange.of("invalid"));
    }

    @Test
    public void missingHeader() {
        Assertions.assertNull(BinaryDataClient.ContentRange.of(null));
    }
}
