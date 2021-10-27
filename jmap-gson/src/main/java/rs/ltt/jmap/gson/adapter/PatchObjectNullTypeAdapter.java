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

package rs.ltt.jmap.gson.adapter;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import rs.ltt.jmap.common.util.Patches;

public class PatchObjectNullTypeAdapter extends TypeAdapter<Patches.Null> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(Patches.Null.class, new PatchObjectNullTypeAdapter());
    }

    @Override
    public void write(JsonWriter jsonWriter, Patches.Null aNull) throws IOException {
        final boolean before = jsonWriter.getSerializeNulls();
        jsonWriter.setSerializeNulls(true);
        jsonWriter.nullValue();
        jsonWriter.setSerializeNulls(before);
    }

    @Override
    public Patches.Null read(JsonReader jsonReader) throws IOException {
        throw new IOException("No deserialization support for PatchObject.NULL");
    }
}
