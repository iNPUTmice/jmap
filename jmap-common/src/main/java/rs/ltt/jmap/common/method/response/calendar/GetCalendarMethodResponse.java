package rs.ltt.jmap.common.method.response.calendar;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Calendar;
import rs.ltt.jmap.common.method.response.standard.GetMethodResponse;

@JmapMethod("Calendar/get")
public class GetCalendarMethodResponse extends GetMethodResponse<Calendar> {

	@Builder
	public GetCalendarMethodResponse(String accountId, String state, String[] notFound, Calendar[] list) {
		super(accountId, state, notFound, list);
	}
}
