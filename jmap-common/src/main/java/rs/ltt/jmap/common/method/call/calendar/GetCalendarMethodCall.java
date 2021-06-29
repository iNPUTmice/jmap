package rs.ltt.jmap.common.method.call.calendar;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Calendar;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("Calendar/get")
public class GetCalendarMethodCall extends GetMethodCall<Calendar> {

	@Builder
	public GetCalendarMethodCall(String accountId, String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
		super(accountId, ids, properties, idsReference);
	}
	
}
