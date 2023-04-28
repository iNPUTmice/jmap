package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarEventStatus {
	@SerializedName("confirmed") CONFIRMED,
	@SerializedName("cancelled") CANCELLED,
	@SerializedName("tentative") TENTATIVE
}
