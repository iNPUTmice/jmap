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

package rs.ltt.jmap.mua.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountUtilTest {

    @Test
    public void simpleUsername() {
        Assertions.assertEquals("Max", AccountUtil.printableName("max@example.com"));
    }

    @Test
    public void allCapsUsername() {
        Assertions.assertEquals("Max", AccountUtil.printableName("MAX@example.com"));
    }

    @Test
    public void dotSeparatedUsername() {
        Assertions.assertEquals("Max User", AccountUtil.printableName("max.user@example.com"));
    }

    @Test
    public void dashSeparatedUsername() {
        Assertions.assertEquals("Max User", AccountUtil.printableName("max-user@example.com"));
    }

    @Test
    public void underscoreSeparatedUsername() {
        Assertions.assertEquals("Max User", AccountUtil.printableName("max_user@example.com"));
    }

    @Test
    public void combinationSeparatedUsername() {
        Assertions.assertEquals("Max User", AccountUtil.printableName("max_-.user@example.com"));
    }

    @Test
    public void noUsername() {
        Assertions.assertEquals("@example.com", AccountUtil.printableName("@example.com"));
    }

    @Test
    public void singleLetterUsername() {
        Assertions.assertEquals("A", AccountUtil.printableName("a@example.com"));
    }

    @Test
    public void singleSeparatorUsername() {
        Assertions.assertEquals(".@example.com", AccountUtil.printableName(".@example.com"));
    }
}
