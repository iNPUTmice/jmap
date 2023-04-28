package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder
@Type("VirtualLocation")
public class CalendarVirtualLocation {
	private String name;
	private String description;
	private String uri;
	
}