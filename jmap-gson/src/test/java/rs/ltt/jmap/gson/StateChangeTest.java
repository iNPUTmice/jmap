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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.*;

import java.io.IOException;
import java.util.Map;

public class StateChangeTest extends AbstractGsonTest {


    @Test
    public void deserialize() throws IOException {
        final StateChange stateChange = parseFromResource("push/state-change.json", StateChange.class);
        final Map<Class<? extends AbstractIdentifiableEntity>, String> max = stateChange.getChanged().get("max@example.com");
        Assertions.assertEquals(2, max.size());
        Assertions.assertEquals("d35ecb040aab", max.get(Email.class));
        Assertions.assertEquals("428d565f2440", max.get(Thread.class));
        final Map<Class<? extends AbstractIdentifiableEntity>, String> sam = stateChange.getChanged().get("sam@example.com");
        Assertions.assertEquals(4, sam.size());
    }

    @Test
    public void serialize() throws IOException {
        final StateChange stateChange = StateChange.builder()
                .changed("max@example.com", ImmutableMap.of(Email.class, "initial"))
                .changed("sam@example.com", ImmutableMap.of(Identity.class, "initial"))
                .build();
        final Gson gson = getGson();
        final String json = gson.toJson(stateChange);
        final String expected = readResourceAsString("push/state-change-out.json");
        Assertions.assertEquals(expected, json);
    }

    @Test
    public void serializeUnknown() throws Exception {
        final StateChange stateChange = StateChange.builder()
                .changed("max@example.com", ImmutableMap.of(UnknownEntity.class, "ignored"))
                .build();
        final Gson gson = getGson();
        Assertions.assertThrows(JsonIOException.class, ()-> gson.toJson(stateChange));

    }

    private static class UnknownEntity extends AbstractIdentifiableEntity {

    }

}
