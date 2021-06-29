package rs.ltt.jmap.common.method.call.calendarevent;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.CalendarEvent;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("CalendarEvent/get")
public class GetCalendarEventMethodCall extends GetMethodCall<CalendarEvent> {

	@Builder
	public GetCalendarEventMethodCall(String accountId, String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
		super(accountId, ids, properties, idsReference);
	}
		
}
