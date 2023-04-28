package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class File {

	private String blobId;
	private String type;
	private String name;
	private Long size;
	
}
