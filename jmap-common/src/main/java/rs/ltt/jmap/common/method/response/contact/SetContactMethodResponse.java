package rs.ltt.jmap.common.method.response.contact;

import java.util.Map;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Contact;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.method.response.standard.SetMethodResponse;

@JmapMethod("Contact/set")
public class SetContactMethodResponse extends SetMethodResponse<Contact> {

	@Builder
	public SetContactMethodResponse(String accountId, String oldState, String newState, Map<String, Contact> created,
			Map<String, Contact> updated, String[] destroyed, Map<String, SetError> notCreated,
			Map<String, SetError> notUpdated, Map<String, SetError> notDestroyed) {
		super(accountId, oldState, newState, created, updated, destroyed, notCreated, notUpdated, notDestroyed);
	}

}
