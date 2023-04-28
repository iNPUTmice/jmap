package rs.ltt.jmap.common.method.response.contact;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Contact;
import rs.ltt.jmap.common.method.response.standard.GetMethodResponse;

@JmapMethod("Contact/get")
public class GetContactMethodResponse extends GetMethodResponse<Contact> {

	@Builder
	public GetContactMethodResponse(String accountId, String state, String[] notFound, Contact[] list) {
		super(accountId, state, notFound, list);
	}
}
