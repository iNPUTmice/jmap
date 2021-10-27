package rs.ltt.jmap.gson;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.gson.adapter.ResultReferenceTypeAdapter;

public class ResultReferenceTypeAdapterTest {

    private static final String METHOD_CALL_ID = "0";

    @Test
    public void writeAndReadBack() {
        Request.Invocation emailQuery =
                new Request.Invocation(
                        QueryEmailMethodCall.builder().accountId("accountId").build(),
                        METHOD_CALL_ID);
        Request.Invocation.ResultReference resultReferenceOut = emailQuery.createReference("/ids");
        GsonBuilder gsonBuilder = new GsonBuilder();
        ResultReferenceTypeAdapter.register(gsonBuilder);
        String json = gsonBuilder.create().toJson(resultReferenceOut);
        Request.Invocation.ResultReference resultReferenceIn =
                gsonBuilder.create().fromJson(json, Request.Invocation.ResultReference.class);
        Assertions.assertEquals(resultReferenceIn.getClazz(), resultReferenceOut.getClazz());
        Assertions.assertEquals(resultReferenceIn.getId(), resultReferenceOut.getId());
        Assertions.assertEquals(resultReferenceIn.getPath(), resultReferenceOut.getPath());
    }
}
