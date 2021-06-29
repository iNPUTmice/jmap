package rs.ltt.jmap.common.entity;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder
@Type("Relation")
public class CalendarRelation {
	private Map<String, Boolean> relation;
}
