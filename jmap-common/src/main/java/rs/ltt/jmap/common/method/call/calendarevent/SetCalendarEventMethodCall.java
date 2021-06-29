package rs.ltt.jmap.common.method.call.calendarevent;

import java.util.Map;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.CalendarEvent;
import rs.ltt.jmap.common.method.call.standard.SetMethodCall;

@JmapMethod("CalendarEvent/set")
public class SetCalendarEventMethodCall extends SetMethodCall<CalendarEvent> {

	@Builder
	public SetCalendarEventMethodCall(String accountId, String ifInState, Map<String, CalendarEvent> create,
									Map<String, Map<String, Object>> update, String[] destroy,
									Request.Invocation.ResultReference destroyReference) {
		super(accountId, ifInState, create, update, destroy, destroyReference);
	}
		
}
