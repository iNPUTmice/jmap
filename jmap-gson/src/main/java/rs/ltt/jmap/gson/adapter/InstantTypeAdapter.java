/*
 * Copyright 2020 Daniel Gultsch
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
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantTypeAdapter extends TypeAdapter<Instant> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
    }

    @Override
    public void write(JsonWriter jsonWriter, Instant instant) throws IOException {
        if (instant == null) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(DateTimeFormatter.ISO_INSTANT.format(instant));
        }
    }

    @Override
    public Instant read(final JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        final String asString = jsonReader.nextString();
        return Instant.parse(asString);
    }
}
