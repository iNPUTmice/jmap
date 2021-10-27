package rs.ltt.jmap.gson;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.util.Patches;

public class SetEmailMethodCallTest extends AbstractGsonTest {

    @Test
    public void emailUpdateTest() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        Gson gson = builder.create();
        Request request =
                new Request.Builder()
                        .call(
                                SetEmailMethodCall.builder()
                                        .accountId("accountId")
                                        .ifInState("state")
                                        .update(
                                                ImmutableMap.of(
                                                        "M123", Patches.remove("keywords/$seen")))
                                        .build())
                        .build();
        Assertions.assertEquals(
                readResourceAsString("request/set-email.json"), gson.toJson(request));
    }
}
