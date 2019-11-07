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
import rs.ltt.jmap.common.Utils;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.MethodResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Mapper {

    private Mapper() {

    }

    public static final ImmutableBiMap<String, Class<? extends MethodCall>> METHOD_CALLS = Mapper.get(MethodCall.class);
    public static final ImmutableBiMap<String, Class<? extends MethodResponse>> METHOD_RESPONSES = Mapper.get(MethodResponse.class);
    public static final ImmutableBiMap<String, Class<? extends MethodErrorResponse>> METHOD_ERROR_RESPONSES = Mapper.get(MethodErrorResponse.class);
    public static final ImmutableBiMap<String, Class<? extends Capability>> CAPABILITIES = Mapper.get(Capability.class);
    public static final ImmutableBiMap<String, Class<? extends AccountCapability>> ACCOUNT_CAPABILITIES = Mapper.get(AccountCapability.class);

    private static <T> ImmutableBiMap<String, Class<? extends T>> get(Class<T> type) {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Mapper.class.getClassLoader().getResourceAsStream(Utils.getFilenameFor(type))));
        final ImmutableBiMap.Builder<String, Class<? extends T>> builder = new ImmutableBiMap.Builder<>();
        try {
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                final String[] parts = line.split(" ", 2);
                if (parts.length == 2) {
                    try {
                        Class<? extends T> clazz = Class.forName(parts[0]).asSubclass(type);
                        builder.put(parts[1], clazz);
                    } catch (ClassNotFoundException | ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

}
