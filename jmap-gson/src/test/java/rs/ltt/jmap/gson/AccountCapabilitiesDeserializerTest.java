package rs.ltt.jmap.gson;

import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.SubmissionAccountCapability;
import rs.ltt.jmap.common.entity.capability.VacationResponseAccountCapability;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.Assert.*;

public class AccountCapabilitiesDeserializerTest extends AbstractGsonTest {
    private static final Type TYPE = new TypeToken<Map<Class<? extends AccountCapability>, AccountCapability>>() {}.getType();

    @Test
    public void mailAccountCapability() throws Exception {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                parseFromResource("account-capability/mail.json", TYPE);

        assertTrue(accountCapabilities.containsKey(MailAccountCapability.class));
        AccountCapability accountCapability = accountCapabilities.get(MailAccountCapability.class);
        assertEquals(MailAccountCapability.class, accountCapability.getClass());
        MailAccountCapability mailAccountCapability = (MailAccountCapability) accountCapability;
        assertEquals(Long.valueOf(20), mailAccountCapability.getMaxMailboxesPerEmail());
        assertEquals(Long.valueOf(10), mailAccountCapability.getMaxMailboxDepth());
        assertEquals(200, mailAccountCapability.getMaxSizeMailboxName());
        assertEquals(50_000_000, mailAccountCapability.getMaxSizeAttachmentsPerEmail());
        assertArrayEquals(new String[] { "receivedAt" }, mailAccountCapability.getEmailQuerySortOptions());
        assertTrue(mailAccountCapability.isMayCreateTopLevelMailbox());
    }

    @Test
    public void submissionAccountCapability() throws Exception {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                parseFromResource("account-capability/submission.json", TYPE);

        assertTrue(accountCapabilities.containsKey(SubmissionAccountCapability.class));
        AccountCapability accountCapability = accountCapabilities.get(SubmissionAccountCapability.class);
        assertEquals(SubmissionAccountCapability.class, accountCapability.getClass());
        SubmissionAccountCapability submissionAccountCapability = (SubmissionAccountCapability) accountCapability;
        assertEquals(0, submissionAccountCapability.getMaxDelayedSend());
        Map<String, String[]> submissionExtensions = submissionAccountCapability.getSubmissionExtensions();
        assertEquals(1, submissionExtensions.size());
        assertTrue(submissionExtensions.containsKey("SIZE"));
        String[] sizeExtensionArguments = submissionExtensions.get("SIZE");
        assertArrayEquals(new String[] { "50000000" }, sizeExtensionArguments);
    }

    @Test
    public void vacationResponseAccountCapability() throws Exception {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                parseFromResource("account-capability/vacation-response.json", TYPE);

        assertTrue(accountCapabilities.containsKey(VacationResponseAccountCapability.class));
        assertEquals(VacationResponseAccountCapability.class, accountCapabilities.get(VacationResponseAccountCapability.class).getClass());
    }

    @Test
    public void allSupportedCapabilitiesAndUnknownCapability() throws Exception {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                parseFromResource("account-capability/all.json", TYPE);

        assertTrue(accountCapabilities.containsKey(MailAccountCapability.class));
        assertTrue(accountCapabilities.containsKey(SubmissionAccountCapability.class));
        assertTrue(accountCapabilities.containsKey(VacationResponseAccountCapability.class));
        assertEquals(3, accountCapabilities.size());
    }
}
