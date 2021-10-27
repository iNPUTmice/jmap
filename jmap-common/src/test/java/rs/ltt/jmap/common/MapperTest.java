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

package rs.ltt.jmap.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.util.Mapper;

public class MapperTest {

    @Test
    public void everyCallHasResponse() {
        for (final String jsonName : Mapper.METHOD_CALLS.inverse().values()) {
            Assertions.assertNotNull(
                    Mapper.METHOD_RESPONSES.get(jsonName),
                    String.format(
                            "Jmap method call %s has no appropriate method response", jsonName));
        }
    }

    @Test
    public void everyResponseHasCall() {
        for (final String jsonName : Mapper.METHOD_RESPONSES.inverse().values()) {
            Assertions.assertNotNull(
                    Mapper.METHOD_CALLS.get(jsonName),
                    String.format(
                            "Jmap method response %s has no appropriate method call", jsonName));
        }
    }
}
