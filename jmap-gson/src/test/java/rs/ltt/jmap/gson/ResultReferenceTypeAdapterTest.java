package rs.ltt.jmap.gson;

import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import rs.ltt.jmap.gson.adapter.ResultReferenceTypeAdapter;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;

public class ResultReferenceTypeAdapterTest {

    private static final String METHOD_CALL_ID = "0";

    @Test
    public void writeAndReadBack() {
        Request.Invocation emailQuery = new Request.Invocation(new QueryEmailMethodCall("accountId"), METHOD_CALL_ID);
        Request.Invocation.ResultReference resultReferenceOut = emailQuery.createReference("/ids");
        GsonBuilder gsonBuilder = new GsonBuilder();
        ResultReferenceTypeAdapter.register(gsonBuilder);
        String json = gsonBuilder.create().toJson(resultReferenceOut);
        Request.Invocation.ResultReference resultReferenceIn = gsonBuilder.create().fromJson(json, Request.Invocation.ResultReference.class);
        Assert.assertEquals(resultReferenceIn.getClazz(), resultReferenceOut.getClazz());
        Assert.assertEquals(resultReferenceIn.getId(), resultReferenceOut.getId());
        Assert.assertEquals(resultReferenceIn.getPath(), resultReferenceOut.getPath());
    }

}
