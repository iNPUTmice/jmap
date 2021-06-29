package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum ContactInformationPhoneType {
	@SerializedName("home") HOME,
	@SerializedName("work") WORK,
	@SerializedName("mobile") MOBILE,
	@SerializedName("fax") FAX,
	@SerializedName("pager") PAGER,
	@SerializedName("other") OTHER
}
