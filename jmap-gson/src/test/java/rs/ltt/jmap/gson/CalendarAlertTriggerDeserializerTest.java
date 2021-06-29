package rs.ltt.jmap.gson;

import org.junit.Assert;
import org.junit.Test;

import rs.ltt.jmap.common.entity.CalendarAbsoluteTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTrigger;
import rs.ltt.jmap.common.entity.CalendarOffsetTriggerRelativeTo;

import java.time.Duration;
import java.time.Instant;

public class CalendarAlertTriggerDeserializerTest extends AbstractGsonTest {
	@Test
	public void offsetTrigger() throws Exception {
		CalendarOffsetTrigger offsetTrigger = parseFromResource("jscalendar-alert-trigger/offset-trigger.json", CalendarOffsetTrigger.class);
		Assert.assertEquals(offsetTrigger.getRelativeTo(), CalendarOffsetTriggerRelativeTo.START);
		Assert.assertEquals(offsetTrigger.getOffset(), Duration.parse("PT1H"));
	}
	
	@Test
	public void absoluteTrigger() throws Exception {
		CalendarAbsoluteTrigger absoluteTrigger = parseFromResource("jscalendar-alert-trigger/absolute-trigger.json", CalendarAbsoluteTrigger.class);
		Assert.assertEquals(absoluteTrigger.getWhen(), Instant.parse("2020-10-20T17:30:00Z"));
	}
}
