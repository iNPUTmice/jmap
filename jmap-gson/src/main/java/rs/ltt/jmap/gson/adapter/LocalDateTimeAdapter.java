package rs.ltt.jmap.gson.adapter;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

	public static void register(final GsonBuilder builder) {
		builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
	}
	
	@Override
	public void write(JsonWriter jsonWriter, LocalDateTime time) throws IOException {
		if (time == null) {
			jsonWriter.nullValue();
		} else {
			jsonWriter.value(time.toString());
		}
	}
	
	@Override
	public LocalDateTime read(final JsonReader jsonReader) throws IOException {
		if (jsonReader.peek() == JsonToken.NULL) {
			jsonReader.nextNull();
			return null;
		}
		final String asString = jsonReader.nextString();
		return LocalDateTime.parse(asString);
	}

}
