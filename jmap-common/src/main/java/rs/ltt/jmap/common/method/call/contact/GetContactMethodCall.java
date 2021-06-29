package rs.ltt.jmap.common.method.call.contact;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Contact;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("Contact/get")
public class GetContactMethodCall extends GetMethodCall<Contact> {

	@Builder
	public GetContactMethodCall(String accountId, String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
		super(accountId, ids, properties, idsReference);
	}

}
