package rs.ltt.jmap.common.method.call.contactgroup;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.ContactGroup;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("ContactGroup/get")
public class GetContactGroupMethodCall extends GetMethodCall<ContactGroup> {

	@Builder
	public GetContactGroupMethodCall(String accountId, String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
		super(accountId, ids, properties, idsReference);
	}

}
