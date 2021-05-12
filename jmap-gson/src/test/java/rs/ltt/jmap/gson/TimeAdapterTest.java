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

package rs.ltt.jmap.gson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

public class TimeAdapterTest extends AbstractGsonTest {

    @Test
    public void deserializeNullInstant() {
        final Example example = getGson().fromJson("{'b': null, 'd':'value'}", Example.class);
        Assertions.assertEquals("value", example.d);
    }

    @Test
    public void deserializeNullOffsetDateTime() {
        final Example example = getGson().fromJson("{'c': null, 'd':'value'}", Example.class);
        Assertions.assertEquals("value", example.d);
    }


    public static final class Example {
        private String a;
        private Instant b;
        private OffsetDateTime c;
        private String d;

        public Example(String a, Instant b, OffsetDateTime c, String d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

}
