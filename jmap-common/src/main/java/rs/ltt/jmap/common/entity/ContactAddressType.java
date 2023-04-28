package rs.ltt.jmap.common.entity;

import com.google.gson.annotations.SerializedName;

public enum ContactAddressType {

	@SerializedName("home") HOME_ADDRESS,
	@SerializedName("work") WORK_ADDRESS,
	@SerializedName("billing") BILLING_ADDRESS,
	@SerializedName("postal") POSTAL_ADDRESS,
	@SerializedName("other") OTHER_ADDRESS
	
}
