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
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.util.Mapper;
import rs.ltt.jmap.common.util.Namespace;

public class PackageNamespaceTest {

    @Test
    public void ensureEveryMethodCallHasNamespace() {
        for (Class<? extends MethodCall> clazz : Mapper.METHOD_CALLS.values()) {
            Assertions.assertNotNull(
                    Namespace.get(clazz),
                    String.format("%s is not defining a package namespace", clazz.getName()));
        }
    }
}
