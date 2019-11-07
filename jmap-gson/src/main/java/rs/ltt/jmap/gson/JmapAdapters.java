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

package rs.ltt.jmap.gson;

import com.google.gson.GsonBuilder;
import rs.ltt.jmap.gson.adapter.PatchObjectNullTypeAdapter;
import rs.ltt.jmap.gson.adapter.RequestInvocationTypeAdapter;
import rs.ltt.jmap.gson.adapter.ResultReferenceTypeAdapter;
import rs.ltt.jmap.gson.deserializer.AccountCapabilitiesDeserializer;
import rs.ltt.jmap.gson.deserializer.CapabilitiesDeserializer;
import rs.ltt.jmap.gson.deserializer.GenericResponseDeserializer;
import rs.ltt.jmap.gson.deserializer.ResponseInvocationDeserializer;
import rs.ltt.jmap.gson.serializer.AccountCapabilitiesSerializer;
import rs.ltt.jmap.gson.serializer.CapabilitiesSerializer;
import rs.ltt.jmap.gson.serializer.ListSerializer;
import rs.ltt.jmap.gson.serializer.StringMapSerializer;


public final class JmapAdapters {

    private JmapAdapters() {

    }

    public static void register(GsonBuilder builder) {

        ResultReferenceTypeAdapter.register(builder);
        PatchObjectNullTypeAdapter.register(builder);

        CapabilitiesDeserializer.register(builder);
        AccountCapabilitiesDeserializer.register(builder);
        GenericResponseDeserializer.register(builder);
        ResponseInvocationDeserializer.register(builder);

        RequestInvocationTypeAdapter.register(builder);
        CapabilitiesSerializer.register(builder);
        AccountCapabilitiesSerializer.register(builder);
        ListSerializer.register(builder);
        StringMapSerializer.register(builder);
    }

}
