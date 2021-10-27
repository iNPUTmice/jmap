package rs.ltt.jmap.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.entity.AccountCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.SubmissionAccountCapability;
import rs.ltt.jmap.common.entity.capability.VacationResponseAccountCapability;

public class AccountCapabilitiesSerializerTest extends AbstractGsonTest {
    private static final Type TYPE =
            new TypeToken<
                    Map<Class<? extends AccountCapability>, AccountCapability>>() {}.getType();

    private final Gson gson = getGson();

    @Test
    public void mailAccountCapability() throws Exception {
        MailAccountCapability mailAccountCapability = createMailAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(mailAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/mail-serialized.json");
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    public void submissionAccountCapability() throws Exception {
        SubmissionAccountCapability submissionAccountCapability =
                createSubmissionAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(submissionAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/submission-serialized.json");
        Assertions.assertEquals(expectedJson, json);
    }

    @Test
    public void vacationResponseAccountCapability() {
        VacationResponseAccountCapability vacationResponseAccountCapability =
                createVacationResponseAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(vacationResponseAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        Assertions.assertEquals("{\"urn:ietf:params:jmap:vacationresponse\":{}}", json);
    }

    @Test
    public void allSupportedCapabilities() throws Exception {
        MailAccountCapability mailAccountCapability = createMailAccountCapability();
        SubmissionAccountCapability submissionAccountCapability =
                createSubmissionAccountCapability();
        VacationResponseAccountCapability vacationResponseAccountCapability =
                createVacationResponseAccountCapability();
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilities =
                createAccountCapabilitiesMap(
                        mailAccountCapability,
                        submissionAccountCapability,
                        vacationResponseAccountCapability);

        String json = gson.toJson(accountCapabilities, TYPE);

        String expectedJson = readResourceAsString("account-capability/all-serialized.json");
        Assertions.assertEquals(expectedJson, json);
    }

    private MailAccountCapability createMailAccountCapability() {
        return MailAccountCapability.builder()
                .maxMailboxesPerEmail(1L)
                .maxMailboxDepth(5L)
                .maxSizeMailboxName(500L)
                .maxSizeAttachmentsPerEmail(10_000_000L)
                .emailQuerySortOptions(new String[] {"receivedAt", "To"})
                .mayCreateTopLevelMailbox(false)
                .build();
    }

    private SubmissionAccountCapability createSubmissionAccountCapability() {
        Map<String, String[]> submissionExtensions = new LinkedHashMap<>();
        submissionExtensions.put("DELIVERBY", new String[] {"240"});
        return SubmissionAccountCapability.builder()
                .maxDelayedSend(300L)
                .submissionExtensions(submissionExtensions)
                .build();
    }

    private VacationResponseAccountCapability createVacationResponseAccountCapability() {
        return VacationResponseAccountCapability.builder().build();
    }

    private Map<Class<? extends AccountCapability>, AccountCapability> createAccountCapabilitiesMap(
            AccountCapability... accountCapabilities) {
        Map<Class<? extends AccountCapability>, AccountCapability> accountCapabilitiesMap =
                new LinkedHashMap<>();
        for (AccountCapability accountCapability : accountCapabilities) {
            accountCapabilitiesMap.put(accountCapability.getClass(), accountCapability);
        }
        return accountCapabilitiesMap;
    }
}
