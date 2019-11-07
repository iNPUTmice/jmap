package rs.ltt.jmap.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.SubmissionAccountCapability;
import rs.ltt.jmap.common.entity.capability.VacationResponseAccountCapability;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AccountCapabilitiesSerializerTest extends AbstractGsonTest {
    private static final Type TYPE = new TypeToken<Map<Class<? extends AccountCapability>, AccountCapability>>() {}.getType();

    private final Gson gson = getGson();

    @Test
    public void mailAccountCapability() throws Exception {
        MailAccountCapability mailAccountCapability = createMailAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(mailAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/mail-serialized.json");
        assertEquals(expectedJson, json);
    }

    @Test
    public void submissionAccountCapability() throws Exception {
        SubmissionAccountCapability submissionAccountCapability = createSubmissionAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(submissionAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/submission-serialized.json");
        assertEquals(expectedJson, json);
    }

    @Test
    public void vacationResponseAccountCapability() {
        VacationResponseAccountCapability vacationResponseAccountCapability = createVacationResponseAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(vacationResponseAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        assertEquals("{\"urn:ietf:params:jmap:vacationresponse\":{}}", json);
    }

    @Test
    public void allSupportedCapabilities() throws Exception {
        MailAccountCapability mailAccountCapability = createMailAccountCapability();
        SubmissionAccountCapability submissionAccountCapability = createSubmissionAccountCapability();
        VacationResponseAccountCapability vacationResponseAccountCapability = createVacationResponseAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(mailAccountCapability, submissionAccountCapability, vacationResponseAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/all-serialized.json");
        assertEquals(expectedJson, json);
    }

    private MailAccountCapability createMailAccountCapability() {
        Long maxMailboxesPerEmail = 1L;
        Long maxMailboxDepth = 5L;
        long maxSizeMailboxName = 500;
        long maxSizeAttachmentsPerEmail = 10_000_000;
        String[] emailQuerySortOptions = new String[] { "receivedAt", "To" };
        boolean mayCreateTopLevelMailbox = false;
        return new MailAccountCapability(maxMailboxesPerEmail, maxMailboxDepth,
                maxSizeMailboxName, maxSizeAttachmentsPerEmail, emailQuerySortOptions, mayCreateTopLevelMailbox);
    }

    private SubmissionAccountCapability createSubmissionAccountCapability() {
        long maxDelayedSend = 300;
        Map<String, String[]> submissionExtensions = new LinkedHashMap<>();
        submissionExtensions.put("DELIVERBY", new String[] { "240" });
        return new SubmissionAccountCapability(maxDelayedSend, submissionExtensions);
    }

    private VacationResponseAccountCapability createVacationResponseAccountCapability() {
        return new VacationResponseAccountCapability();
    }

    private Map<Class<? extends AccountCapability>, AccountCapability> createAccountCapabilitiesMap(AccountCapability... accountCapabilities) {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilitiesMap = new LinkedHashMap<>();
        for (AccountCapability accountCapability : accountCapabilities) {
            accountCapabilitiesMap.put(accountCapability.getClass(), accountCapability);
        }
        return accountCapabilitiesMap;
    }
}
