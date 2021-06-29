package rs.ltt.jmap.common.entity;

import java.time.Duration;

import lombok.Getter;
import lombok.Builder;
import rs.ltt.jmap.annotation.Type;

@Getter
@Type("OffsetTrigger")
public class CalendarOffsetTrigger extends CalendarTrigger {
	
	private Duration offset;
	private CalendarOffsetTriggerRelativeTo relativeTo;

	@Builder(toBuilder = true)
	public CalendarOffsetTrigger(Duration offset,
								 CalendarOffsetTriggerRelativeTo relativeTo) {
		this.offset = offset;
		this.relativeTo = relativeTo;
	}
}
