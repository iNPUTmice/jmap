package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class Calendar extends AbstractIdentifiableEntity {

	private String name;
	private String color;
	private Long sortOrder;
	private boolean isVisible;

	private Map<String, CalendarRights> shareWith;
}

