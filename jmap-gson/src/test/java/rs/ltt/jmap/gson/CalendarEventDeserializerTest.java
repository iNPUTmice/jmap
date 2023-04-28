package rs.ltt.jmap.gson;

import org.junit.Assert;
import org.junit.Test;
import rs.ltt.jmap.common.entity.CalendarEvent;

import static rs.ltt.jmap.gson.AbstractGsonTest.parseFromResource;

public class CalendarEventDeserializerTest extends AbstractGsonTest {

    @Test
	public void calendarEvent() throws Exception {
		CalendarEvent calendarEvent = parseFromResource("calendar-event/calendar-event.json", CalendarEvent.class);
		Assert.assertEquals(calendarEvent.getCalendarId(), "123");
	}
}
