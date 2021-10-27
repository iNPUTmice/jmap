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

package rs.ltt.jmap.mua.cache;

import com.google.common.base.MoreObjects;
import java.util.List;

public class Missing {

    public final String threadState;
    public final String emailState;
    public final List<String> threadIds;

    public Missing(String threadState, String emailState, List<String> threadIds) {
        this.threadState = threadState;
        this.emailState = emailState;
        this.threadIds = threadIds;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("threadState", threadState)
                .add("emailState", emailState)
                .add("threadIds", threadIds)
                .toString();
    }
}
