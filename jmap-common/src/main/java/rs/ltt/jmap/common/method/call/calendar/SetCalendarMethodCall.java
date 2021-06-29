package rs.ltt.jmap.common.method.call.calendar;

import java.util.Map;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Calendar;
import rs.ltt.jmap.common.method.call.standard.SetMethodCall;

@JmapMethod("Calendar/set")
public class SetCalendarMethodCall extends SetMethodCall<Calendar> {

	@Builder
	public SetCalendarMethodCall(String accountId, String ifInState, Map<String, Calendar> create,
								Map<String, Map<String, Object>> update, String[] destroy,
								Request.Invocation.ResultReference destroyReference) {
		super(accountId, ifInState, create, update, destroy, destroyReference);
	}

}
