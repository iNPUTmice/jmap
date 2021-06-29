package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarParticipantRole {
	@SerializedName("owner") OWNER,
	@SerializedName("attendee") ATTENDEE,
	@SerializedName("optional") OPTIONAL,
	@SerializedName("informational") INFORMATIONAL,
	@SerializedName("chair") CHAIR
}
