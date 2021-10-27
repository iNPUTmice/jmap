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
import rs.ltt.jmap.client.blob.Download;

public class DownloadTest {

    @Test
    public void progressTest() {
        final Download download = new Download(null, true, 2000, null);
        Assertions.assertEquals(0, download.progress(0));
        Assertions.assertEquals(1, download.progress(20));
        Assertions.assertEquals(1, download.progress(21));
        Assertions.assertEquals(50, download.progress(1000));
        Assertions.assertEquals(100, download.progress(2000));
        Assertions.assertEquals(100, download.progress(2002));
        Assertions.assertEquals(100, download.progress(3000));
    }
}
