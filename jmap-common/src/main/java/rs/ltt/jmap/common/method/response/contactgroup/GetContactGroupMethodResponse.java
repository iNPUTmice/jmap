package rs.ltt.jmap.common.method.response.contactgroup;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Contact;
import rs.ltt.jmap.common.entity.ContactGroup;
import rs.ltt.jmap.common.method.response.standard.GetMethodResponse;

@JmapMethod("ContactGroup/get")
public class GetContactGroupMethodResponse extends GetMethodResponse<ContactGroup> {

	@Builder
	public GetContactGroupMethodResponse(String accountId, String state, String[] notFound, ContactGroup[] list) {
		super(accountId, state, notFound, list);
	}
}
