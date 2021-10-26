package rs.ltt.jmap.common.method.response.calendarevent;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.CalendarEvent;
import rs.ltt.jmap.common.method.response.standard.GetMethodResponse;

@JmapMethod("CalendarEvent/get")
public class GetCalendarEventMethodResponse extends GetMethodResponse<CalendarEvent> {

	@Builder
	public GetCalendarEventMethodResponse(String accountId, String state, String[] notFound, CalendarEvent[] list) {
		super(accountId, state, notFound, list);
	}
}
