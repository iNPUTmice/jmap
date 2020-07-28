package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarParticipationStatus {
	@SerializedName("needs-action") NEEDS_ACTION,
	@SerializedName("accepted") ACCEPTED,
	@SerializedName("declined") DECLINED,
	@SerializedName("tentative") TENTATIVE,
	@SerializedName("delegated") DELEGATED
}
