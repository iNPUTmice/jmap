package rs.ltt.jmap.gson.deserializer;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import rs.ltt.jmap.common.entity.CalendarAbsoluteTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTrigger;
import rs.ltt.jmap.common.entity.CalendarTrigger;
import rs.ltt.jmap.common.entity.CalendarUnknownTrigger;

public class CalendarAlertTriggerDeserializer implements JsonDeserializer<CalendarTrigger> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(CalendarTrigger.class, new CalendarAlertTriggerDeserializer());
    }

    @Override
    public CalendarTrigger deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("@type")) {
                /*
                 * Store the value of the property "@type" in the variable typeValue,
                 * in order to be able to differentiate and deserialize based upon it
                 */
                String typeValue = jsonObject.get("@type").getAsString();

                switch (typeValue) {
                    case "AbsoluteTrigger":
                        // Parse as JSCalendarAbsoluteTrigger
                        return context.deserialize(jsonObject, CalendarAbsoluteTrigger.class);

                    case "OffsetTrigger":
                        // Parse as JSCalendarOffsetTrigger
                        return context.deserialize(jsonObject, CalendarOffsetTrigger.class);

                    default:
                        // Parse as JSCalendarUnknownTrigger
                        return context.deserialize(jsonObject, CalendarUnknownTrigger.class);
                }
            }

            throw new JsonParseException("Unable to identify type of trigger. Property \"@type\" is missing");
        } else {
            throw new JsonParseException("Unexpected JSON type when parsing trigger");
        }
    }

}