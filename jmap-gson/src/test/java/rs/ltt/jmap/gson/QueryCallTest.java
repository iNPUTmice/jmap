package rs.ltt.jmap.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;

import java.io.IOException;

public class QueryCallTest extends AbstractGsonTest {

    @Test
    public void queryCallTest() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        Gson gson = builder.create();
        final EmailQuery query = EmailQuery.of(EmailFilterCondition.builder().inMailbox("inbox-id").build(), true);
        Assert.assertEquals(readResourceAsString("request/query-email.json"), gson.toJson(new QueryEmailMethodCall(query)));
        Assert.assertEquals(readResourceAsString("request/query-changes-email.json"), gson.toJson(new QueryChangesEmailMethodCall("accountId", "first", query)));
    }

}
