package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum CalendarRecurrenceFrequency {
	@SerializedName("yearly") YEARLY,
	@SerializedName("monthly") MONTHLY,
	@SerializedName("weekly") WEEKLY,
	@SerializedName("daily") DAILY,
	@SerializedName("hourly") HOURLY,
	@SerializedName("minutely") MINUTELY,
	@SerializedName("secondly") SECONDLY
}
