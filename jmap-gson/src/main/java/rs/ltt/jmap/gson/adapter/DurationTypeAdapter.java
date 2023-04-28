package rs.ltt.jmap.gson.adapter;

import java.io.IOException;
import java.time.Duration;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DurationTypeAdapter extends TypeAdapter<Duration> {

	public static void register(final GsonBuilder builder) {
		builder.registerTypeAdapter(Duration.class, new DurationTypeAdapter());
	}
	
	@Override
	public void write(JsonWriter jsonWriter, Duration duration) throws IOException {
		if (duration == null) {
			jsonWriter.nullValue();
		} else {
			jsonWriter.value(duration.toString());
		}
	}
	
	@Override
	public Duration read(final JsonReader jsonReader) throws IOException {
		if (jsonReader.peek() == JsonToken.NULL) {
			jsonReader.nextNull();
			return null;
		}
		final String asString = jsonReader.nextString();
		return Duration.parse(asString);
	}

}
