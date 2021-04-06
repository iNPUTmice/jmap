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
import rs.ltt.jmap.gson.adapter.*;
import rs.ltt.jmap.gson.deserializer.*;
import rs.ltt.jmap.gson.serializer.*;


public final class JmapAdapters {

    private JmapAdapters() {

    }

    public static void register(GsonBuilder builder) {

        builder.registerTypeAdapterFactory(new TypeInjectionAdapterFactory());

        InstantTypeAdapter.register(builder);
        OffsetDateTimeTypeAdapter.register(builder);
        ResultReferenceTypeAdapter.register(builder);
        PatchObjectNullTypeAdapter.register(builder);
        TypeStateMapAdapter.register(builder);

        CapabilitiesDeserializer.register(builder);
        FilterDeserializer.register(builder);
        AccountCapabilitiesDeserializer.register(builder);
        PrimaryAccountsDeserializer.register(builder);
        GenericResponseDeserializer.register(builder);
        ResponseInvocationDeserializer.register(builder);

        RequestInvocationTypeAdapter.register(builder);
        CapabilitiesSerializer.register(builder);
        FilterSerializer.register(builder);
        AccountCapabilitiesSerializer.register(builder);
        PrimaryAccountsSerializer.register(builder);
        ListSerializer.register(builder);
        ResponseInvocationSerializer.register(builder);
        StringMapSerializer.register(builder);
    }

}
