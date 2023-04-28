package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CalendarNDay {

	private CalendarDays day;
	private Long nthOfPeriod;
	
}
