package rs.ltt.jmap.common.entity;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder
@Type("Location")
public class CalendarLocation {
	private String name;
	private String description;
	private String relativeTo;
	private String timeZone;
	private String coordinates;
	private Map<String, Boolean> linkIds;
	
}
