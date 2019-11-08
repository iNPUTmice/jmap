package rs.ltt.jmap.gson;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.util.Patches;

import java.io.IOException;


public class SetEmailMethodCallTest extends AbstractGsonTest {

    @Test
    public void emailUpdateTest() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        Gson gson = builder.create();
        Request request = new Request.Builder().call(new SetEmailMethodCall("accountId", "state", ImmutableMap.of("M123", Patches.remove("keywords/$seen")))).build();
        Assert.assertEquals(readResourceAsString("request/set-email.json"),gson.toJson(request));
    }

}
