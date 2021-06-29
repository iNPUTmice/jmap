package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ContactInformationOnline extends ContactInformation {
	private final ContactInformationOnlineType type;

	@Builder(toBuilder = true)
	public ContactInformationOnline(String label, String value, boolean isDefault, ContactInformationOnlineType type) {
		this.label = label;
		this.value = value;
		this.isDefault = isDefault;
		this.type = type;
	}
}
