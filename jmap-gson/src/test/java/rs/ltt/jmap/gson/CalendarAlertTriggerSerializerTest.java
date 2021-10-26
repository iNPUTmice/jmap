package rs.ltt.jmap.gson;

import org.junit.Test;

import com.google.gson.Gson;

import rs.ltt.jmap.common.entity.CalendarAbsoluteTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTriggerRelativeTo;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class CalendarAlertTriggerSerializerTest extends AbstractGsonTest {
	
	private final Gson gson = getGson();
	
	@Test
	public void offsetTrigger() throws Exception {
		CalendarOffsetTrigger offsetTrigger = createJSCalendarOffsetTrigger();
		
		String json = gson.toJson(offsetTrigger, CalendarOffsetTrigger.class);
		
		String expectedJson = readResourceAsString("jscalendar-alert-trigger/offset-trigger.json");
		assertEquals(expectedJson, json);
	}
	
	@Test
	public void absoluteTrigger() throws Exception {
		CalendarAbsoluteTrigger absoluteTrigger = createJSCalendarAbsoluteTrigger();
		
		String json = gson.toJson(absoluteTrigger, CalendarAbsoluteTrigger.class);
		
		String expectedJson = readResourceAsString("jscalendar-alert-trigger/absolute-trigger.json");
		assertEquals(expectedJson, json);
	}
	
	private CalendarOffsetTrigger createJSCalendarOffsetTrigger() {
		return CalendarOffsetTrigger.builder()
				.offset(Duration.parse("PT1H"))
				.relativeTo(CalendarOffsetTriggerRelativeTo.START)
				.build();
	}
	
	private CalendarAbsoluteTrigger createJSCalendarAbsoluteTrigger() {
		return CalendarAbsoluteTrigger.builder()
				.when(Instant.parse("2020-10-20T17:30:00Z"))
				.build();
	}
	
}
