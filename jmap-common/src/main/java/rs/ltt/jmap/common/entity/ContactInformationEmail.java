package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

@Getter
public class ContactInformationEmail extends ContactInformation {
	private final ContactInformationEmailType type;

	@Builder(toBuilder = true)
	public ContactInformationEmail(String label, String value, boolean isDefault, ContactInformationEmailType type) {
		this.label = label;
		this.value = value;
		this.isDefault = isDefault;
		this.type = type;
	}
}
