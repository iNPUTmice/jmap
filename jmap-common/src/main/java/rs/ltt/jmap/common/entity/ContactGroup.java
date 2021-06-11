package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ContactGroup extends AbstractIdentifiableEntity {

    private String name;
    private String[] contactIds;

}
