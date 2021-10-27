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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeTypeAdapter extends TypeAdapter<OffsetDateTime> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter());
    }

    @Override
    public void write(final JsonWriter jsonWriter, final OffsetDateTime offsetDateTime)
            throws IOException {
        if (offsetDateTime == null) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime));
        }
    }

    @Override
    public OffsetDateTime read(final JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        final String asString = jsonReader.nextString();
        return OffsetDateTime.parse(asString);
    }
}
