package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Type("UnknownTrigger")
public class CalendarUnknownTrigger extends CalendarTrigger {

    @Builder(toBuilder = true)
    public CalendarUnknownTrigger() {}
}
