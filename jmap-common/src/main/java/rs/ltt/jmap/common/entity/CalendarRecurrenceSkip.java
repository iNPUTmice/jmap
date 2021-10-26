package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarRecurrenceSkip {
	@SerializedName("omit") OMIT,
	@SerializedName("backward") BACKWARD,
	@SerializedName("forward") FORWARD
}
