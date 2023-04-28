package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder(toBuilder = true)
@Type("CalendarRights")
public class CalendarRights {

    private boolean mayReadFreeBusy;
    private boolean mayReadItems;
    private boolean mayAddItems;
    private boolean mayUpdatePrivate;
    private boolean mayRSVP;
    private boolean mayUpdateOwn;
    private boolean mayUpdateAll;
    private boolean mayRemoveOwn;
    private boolean mayRemoveAll;
    private boolean mayAdmin;
    private boolean mayDelete;

}
