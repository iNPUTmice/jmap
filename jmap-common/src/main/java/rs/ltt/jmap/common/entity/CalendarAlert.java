package rs.ltt.jmap.common.entity;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder(toBuilder = true)
@Type("Alert")
public class CalendarAlert {
	private CalendarTrigger trigger;
	
	private String acknowledged;
	
	private Map<String, CalendarRelation> relatedTo;
	
	private CalendarAlertAction action;
	
}
