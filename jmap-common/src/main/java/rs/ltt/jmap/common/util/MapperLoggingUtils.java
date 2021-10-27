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

package rs.ltt.jmap.common.util;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;

public class MapperLoggingUtils {

    private static final Map<Class<?>, Class<?>> WELL_KNOWN_MAPPINGS;

    static {
        WELL_KNOWN_MAPPINGS =
                new ImmutableMap.Builder<Class<?>, Class<?>>()
                        .put(MethodCall.class, EchoMethodCall.class)
                        .put(MethodResponse.class, EchoMethodResponse.class)
                        .put(MethodErrorResponse.class, UnknownMethodMethodErrorResponse.class)
                        .put(Capability.class, CoreCapability.class)
                        .put(AccountCapability.class, MailAccountCapability.class)
                        .build();
    }

    public static <T> boolean isMissingWellKnown(
            ImmutableBiMap<String, Class<? extends T>> map, Class<T> type) {
        final Class<?> wellKnownMapping = WELL_KNOWN_MAPPINGS.get(type);
        return wellKnownMapping != null && !map.containsValue(wellKnownMapping);
    }
}
