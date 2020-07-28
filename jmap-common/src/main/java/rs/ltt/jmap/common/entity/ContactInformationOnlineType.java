package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum ContactInformationOnlineType {
	@SerializedName("uri") URI,
	@SerializedName("username") USERNAME,
	@SerializedName("other") OTHER
}
