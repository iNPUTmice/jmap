package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Calendar extends AbstractIdentifiableEntity {

	private String name;
	private String color;
	private Long sortOrder;
	private boolean isVisible;
	private boolean mayReadFreeBusy;
	private boolean mayReadItems;
	private boolean mayAddItems;
	private boolean mayModifyItems;
	private boolean mayRemoveItems;
	private boolean mayRename;
	private boolean mayDelete;
	
}

