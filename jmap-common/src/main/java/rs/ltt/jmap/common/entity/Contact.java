package rs.ltt.jmap.common.entity;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder(toBuilder = true)
public class Contact extends AbstractIdentifiableEntity {
	
	private String addressBookId;
	private boolean isFlagged;
	private File avatar;
	private String prefix;
	private String firstName;
	private String lastName;
	private String suffix;
	private String nickname;
	private String birthday;
	private String anniversary;
	private String company;
	private String department;
	private String jobTitle;
	
	@Singular
	private List<ContactInformationEmail> emails;
	
	@Singular
	private List<ContactInformationPhone> phones;
	
	@Singular("online")
	private List<ContactInformationOnline> online;
	
	@Singular
	private List<ContactAddress> addresses;
	private String notes;
	private String uid;
	
}
