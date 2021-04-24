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
import rs.ltt.jmap.client.event.State;

import java.util.Arrays;

public class StateTest {

    @Test
    public void testReduceToFailed() {
        Assertions.assertEquals(
                State.FAILED,
                State.reduce(Arrays.asList(State.CLOSED, State.CONNECTED, State.FAILED, State.CLOSED))
        );
    }

    @Test
    public void testReduceToClosed() {
        Assertions.assertEquals(
                State.CLOSED,
                State.reduce(Arrays.asList(State.CLOSED, State.CONNECTED, State.CONNECTING, State.CLOSED))
        );
    }

    @Test
    public void testReduceToConnecting() {
        Assertions.assertEquals(
                State.CONNECTING,
                State.reduce(Arrays.asList(State.CONNECTED, State.CONNECTING))
        );
    }

    @Test
    public void testReduceToConnected() {
        Assertions.assertEquals(
                State.CONNECTED,
                State.reduce(Arrays.asList(State.CONNECTED, State.CONNECTED))
        );
    }

}
