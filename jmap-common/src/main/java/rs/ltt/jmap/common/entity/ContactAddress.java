package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactAddress {

	private ContactAddressType type;
	private String label;
	private String street;
	private String locality;
	private String region;
	private String postcode;
	private String country;
	private boolean isDefault;
	
}
