/*
 * Copyright 2022 Daniel Gultsch
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

package rs.ltt.jmap.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;

public class EmailFilterConditionTest {

    @Test
    public void headerZeroElements() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> EmailFilterCondition.builder().header(new String[0]).build());
    }

    @Test
    public void headerThreeElements() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        EmailFilterCondition.builder()
                                .header(new String[] {"one", "two", "three"})
                                .build());
    }

    @Test
    public void header() {
        EmailFilterCondition.builder().header(new String[] {"foo"}).build();
        EmailFilterCondition.builder().header(new String[] {"foo", "bar"}).build();
    }
}
