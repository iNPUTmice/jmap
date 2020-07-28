package rs.ltt.jmap.common.method.call.contact;

import java.util.Map;

import lombok.Builder;

import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Contact;
import rs.ltt.jmap.common.method.call.standard.SetMethodCall;

@JmapMethod("Contact/set")
public class SetContactMethodCall extends SetMethodCall<Contact> {

	@Builder
	public SetContactMethodCall(String accountId, String ifInState, Map<String, Contact> create,
								Map<String, Map<String, Object>> update, String[] destroy,
								Request.Invocation.ResultReference destroyReference) {
		super(accountId, ifInState, create, update, destroy, destroyReference);
	}

}
