package rs.ltt.jmap.gson.serializer;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import rs.ltt.jmap.common.entity.CalendarAbsoluteTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTrigger;
import rs.ltt.jmap.common.entity.CalendarTrigger;
import rs.ltt.jmap.common.entity.CalendarUnknownTrigger;

public class CalendarAlertTriggerSerializer implements JsonSerializer<CalendarTrigger> {

    public static void register(final GsonBuilder builder) {
        builder.registerTypeAdapter(new TypeToken<CalendarAbsoluteTrigger>() {
        }.getType(), new CalendarAlertTriggerSerializer());
        builder.registerTypeAdapter(new TypeToken<CalendarOffsetTrigger>() {
        }.getType(), new CalendarAlertTriggerSerializer());
        builder.registerTypeAdapter(new TypeToken<CalendarUnknownTrigger>() {
        }.getType(), new CalendarAlertTriggerSerializer());
    }

    @Override
    public JsonElement serialize(CalendarTrigger trigger, Type type, JsonSerializationContext context) {
        if (trigger == null) {
            return null;
        }

        JsonObject jsonObject = new JsonObject();

        if (trigger instanceof CalendarOffsetTrigger) {
            jsonObject.add("@type", context.serialize("OffsetTrigger"));
            jsonObject.add("offset", context.serialize(((CalendarOffsetTrigger) trigger).getOffset().toString()));
            jsonObject.add("relativeTo", context.serialize(((CalendarOffsetTrigger) trigger).getRelativeTo()));

            return jsonObject;
        } else if (trigger instanceof CalendarAbsoluteTrigger) {
            jsonObject.add("@type", context.serialize("AbsoluteTrigger"));
            jsonObject.add("when", context.serialize(((CalendarAbsoluteTrigger) trigger).getWhen().toString()));

            return jsonObject;
        } else {
            return jsonObject;
        }
    }

}