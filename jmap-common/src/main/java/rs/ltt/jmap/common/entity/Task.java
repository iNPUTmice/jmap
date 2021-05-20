package rs.ltt.jmap.common.entity;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Type("jstask")
public class Task extends AbstractIdentifiableEntity{

    // Defined in https://www.ietf.org/archive/id/draft-ietf-jmap-tasks-00.html#name-tasks
    private String taskListId;
    private boolean isDraft;
    private Instant utcStart;
    private Instant utcDue;
    private int sortOrder;

    //COMMONPROPERTIES
    //metadata properties

    private String uid;
    private Map<String, CalendarRelation> relatedTo;
    private String prodId;
    private Instant created;
    private Instant updated;
    private Long sequence;
    private String method;

    //What and Where Properties
    private String title;
    private String description;
    private String descriptionContentType;
    private Boolean showWithoutTime;
    private Map<String, CalendarLocation> locations;
    private Map<String, CalendarVirtualLocation> virtualLocations;
    private Map<String, CalendarLink> links;
    private String locale;
    private Map<String, Boolean> keywords;
    private Map<String, Boolean> categories;
    private String color;

    //recurrence properties
    private LocalDateTime recurrenceId;
    private CalendarRecurrenceRule recurrenceRule;
    private CalendarRecurrenceRule excludedRecurrenceRule;
    private Map<LocalDateTime, Map<String, Object>> recurrenceOverrides;
    private Boolean excluded;

    //Sharing and scheduling properties
    private Long priority;
    private CalendarFreeBusyStatus freeBusyStatus;
    private String privacy;
    private Map<CalendarReplyToMethod, String> replyTo;
    private Map<String, CalendarParticipant> participants;

    //arets properties
    private Boolean useDefaultAlerts;
    private Map<String, CalendarAlert> alerts;

    //Multilingual Properties
    //todo localizations

    //time zone properties
    private String timeZone;
    //todo timeZones

    //JSTask Properties
    private LocalDateTime due;
    private LocalDateTime start;
    private Duration estimatedDuration;
    private int percentComplete;
    private String progress;
    private Instant progressUpdated;

}
