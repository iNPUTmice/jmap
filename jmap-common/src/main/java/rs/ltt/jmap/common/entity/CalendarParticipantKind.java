package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarParticipantKind {
	@SerializedName("individual") INDIVIDUAL,
	@SerializedName("group") GROUP,
	@SerializedName("resource") RESOURCE
}
