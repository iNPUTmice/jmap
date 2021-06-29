package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarProgressType {
	@SerializedName("needs-action") NEEDS_ACTION,
	@SerializedName("in-process") IN_PROCESS,
	@SerializedName("completed") COMPLETED,
	@SerializedName("failed") FAILED,
	@SerializedName("cancelled") CANCELLED
}
