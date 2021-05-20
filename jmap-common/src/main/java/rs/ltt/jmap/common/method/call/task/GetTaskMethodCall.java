package rs.ltt.jmap.common.method.call.task;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.entity.Task;
import rs.ltt.jmap.common.method.call.standard.GetMethodCall;

@JmapMethod("Task/get")
public class GetTaskMethodCall extends GetMethodCall<Task> {

	@Builder
	public GetTaskMethodCall(String accountId, String[] ids, String[] properties, Request.Invocation.ResultReference idsReference) {
		super(accountId, ids, properties, idsReference);
	}
		
}
