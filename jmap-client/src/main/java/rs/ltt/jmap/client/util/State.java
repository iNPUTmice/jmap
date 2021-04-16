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

package rs.ltt.jmap.client.util;

import java.util.Arrays;
import java.util.List;

public enum State {


    //TODO do we want to add CLOSING and regard CONNECTING and CLOSING as some inbetween states?

    CLOSED, CONNECTING, CONNECTED, FAILED;

    private static final List<State> STATES_NEEDING_RECONNECT = Arrays.asList(State.CLOSED, State.FAILED);

    public boolean needsReconnect() {
        return STATES_NEEDING_RECONNECT.contains(this);
    }

}
