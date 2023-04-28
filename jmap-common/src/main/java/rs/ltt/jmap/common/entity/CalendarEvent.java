package rs.ltt.jmap.common.entity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import rs.ltt.jmap.annotation.Type;

@Getter
@Builder(toBuilder = true)
@Type("jsevent")
public class CalendarEvent extends AbstractIdentifiableEntity {

	/** 
	 * Below are CalendarEvent specific properties
	 * Some of them are from an older version of JMAP Specs (a version before this commit with lots of changes regarding contacts and calendars: https://github.com/jmapio/jmap/commit/2178bc51b766d7200dbdc7f1c371b14dcb39ff99#diff-bddbb9a42f817e5d1f1efd168f7aa529b41b0d6470511426a28e6658fa3f1b43),
	 * but are still currently used by FastMail
	 */
	private String calendarId;
	private String participantId;
	
	
	/**
	 * Below are CalendarEvent properties, inherited from JSEvent (which is from the JSCalendar RFC)
	 * See further info: https://tools.ietf.org/html/draft-ietf-calext-jscalendar-32
	 * Also from an older version of JSCalendar (https://tools.ietf.org/html/draft-ietf-calext-jscalendar-19), currently supported and used in FastMail
	 * Note: the commented properties are as per JSEvent, but not used by FastMail (see at the end of the class)
	*/
	
	/**
	 * This attribute is of type "LocalDateTime" according to the JSCalendar specs
	 * ("LocalDateTime" is essentially a string in a specific format)
	 * See also: https://tools.ietf.org/html/draft-ietf-calext-jscalendar-32#section-1.4.5
	*/
	private LocalDateTime start;
	
	/**
	 * This attribute is of type "Duration" (which is also a string in a specific format)
	 * See also: https://tools.ietf.org/html/draft-ietf-calext-jscalendar-32#section-1.4.6
	*/
	private Duration duration;
	
	private CalendarEventStatus status;
	private String uid;
	private Map<String, CalendarRelation> relatedTo;
	private String prodId;
	
	// This is of type "UTCDateTime" (also a string in a special format)
	// See (https://tools.ietf.org/html/draft-ietf-calext-jscalendar-32#section-1.4.4)"
	private Instant created;
	
	// This is also of type "UTCDateTime"
	private Instant updated;
	
	private Long sequence;
	
	private String title;
	private String description;
	private String descriptionContentType;
	private Boolean showWithoutTime;
	private Map<String, CalendarLocation> locations;
	private Map<String, CalendarVirtualLocation> virtualLocations;
	private Map<String, CalendarLink> links;
	private String locale;
	private Map<String, Boolean> keywords;
	
	
	
	
	
	private CalendarRecurrenceRule recurrenceRule;
	
	/** This is of type "LocalDateTime[PatchObject]" where PatchObject is of type String[*]
	 * The conversion between "LocalDateTime[PatchObject]" is based on the following mappings:
	 * LocalDateTime <-> String and PatchObject <-> Map<String, Object>, since LocalDateTime is essentially a string
	 * and PatchObject is essentially a map of String to Object
	*/ 
	private Map<LocalDateTime, Map<String, Object>> recurrenceOverrides;
	
	private Boolean excluded;
	private Long priority;
	private CalendarFreeBusyStatus freeBusyStatus;
	private String privacy;
	private Map<CalendarReplyToMethod, String> replyTo;
	private Map<String, CalendarParticipant> participants;
	private Boolean useDefaultAlerts;
	private Map<String, CalendarAlert> alerts;
	private String timeZone;
	
}
