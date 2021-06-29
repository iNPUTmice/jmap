package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarScheduleAgentType {
	@SerializedName("server") SERVER,
	@SerializedName("client") CLIENT,
	@SerializedName("none") NONE
}
