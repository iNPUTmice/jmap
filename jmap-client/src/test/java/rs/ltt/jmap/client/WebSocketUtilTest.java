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
import rs.ltt.jmap.client.util.WebSocketUtil;

public class WebSocketUtilTest {

    @Test
    public void regularWss() {
        final String input = "wss://localhost/path";
        Assertions.assertEquals(
                "https://localhost/path",
                WebSocketUtil.normalizeUrl(input).toString()
        );
    }

    @Test
    public void regularWs() {
        final String input = "ws://localhost/path";
        Assertions.assertEquals(
                "http://localhost/path",
                WebSocketUtil.normalizeUrl(input).toString()
        );
    }

    @Test
    public void unknownScheme() {
        Assertions.assertThrows(IllegalArgumentException.class,() ->{
           WebSocketUtil.normalizeUrl("unknown://localhost/path");
        });
    }

}
