package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ContactInformationPhone extends ContactInformation {
	private final ContactInformationPhoneType type;

	@Builder(toBuilder = true)
	public ContactInformationPhone(String label, String value, boolean isDefault, ContactInformationPhoneType type) {
		this.label = label;
		this.value = value;
		this.isDefault = isDefault;
		this.type = type;
	}
}
