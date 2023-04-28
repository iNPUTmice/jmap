package rs.ltt.jmap.common.entity;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder
@Type("RecurrenceRule")
public class CalendarRecurrenceRule {
	private CalendarRecurrenceFrequency frequency;
	private Long interval;
	private String rscale;
	private CalendarRecurrenceSkip skip;
	private CalendarDays firstDayOfWeek;
	private List<CalendarNDay> byDay;
	private List<Integer> byMonthDay;
	private List<CalendarMonths> byMonth;
	private List<Integer> byYearDay;
	private List<Integer> byWeekNo;
	private List<Integer> byHour;
	private List<Integer> byMinute;
	private List<Integer> bySecond;
	private List<Integer> bySetPosition;
	private Long count;
	
	// This is of type "LocalDateTime"
	private LocalDateTime until;
	
}
