package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarParticipantAttendance {
	@SerializedName("none") NONE,
	@SerializedName("optional") OPTIONAL,
	@SerializedName("required") REQUIRED
}
