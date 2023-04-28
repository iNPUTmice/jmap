package rs.ltt.jmap.common.method.response.task;

import lombok.Builder;
import rs.ltt.jmap.annotation.JmapMethod;
import rs.ltt.jmap.common.entity.Task;
import rs.ltt.jmap.common.method.response.standard.GetMethodResponse;

@JmapMethod("Task/get")
public class GetTaskMethodResponse extends GetMethodResponse<Task> {

	@Builder
	public GetTaskMethodResponse(String accountId, String state, String[] notFound, Task[] list) {
		super(accountId, state, notFound, list);
	}
}