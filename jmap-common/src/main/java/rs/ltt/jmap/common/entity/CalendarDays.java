package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarDays {
	@SerializedName("mo") MONDAY,
	@SerializedName("tu") TUESDAY,
	@SerializedName("we") WEDNESDAY,
	@SerializedName("th") THURSDAY,
	@SerializedName("fr") FRIDAY,
	@SerializedName("sa") SATURDAY,
	@SerializedName("su") SUNDAY
}
