package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ContactInformation {
	protected String label;
	protected String value;
	protected boolean isDefault;
}
