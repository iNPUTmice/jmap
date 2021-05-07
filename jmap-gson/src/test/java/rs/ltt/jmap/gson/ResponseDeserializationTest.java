package rs.ltt.jmap.gson;

import com.google.gson.JsonParseException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.error.RequestTooLargeMethodErrorResponse;
import rs.ltt.jmap.gson.deserializer.ResponseInvocationDeserializer;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;

public class ResponseDeserializationTest extends AbstractGsonTest {

    @Test
    public void deserializeMailboxGetEmailGetResponse() throws IOException {
        GenericResponse genericResponse = parseFromResource("response/mailbox-get-email-get.json", GenericResponse.class);
        MatcherAssert.assertThat(genericResponse, instanceOf(Response.class));
        final Response response = (Response) genericResponse;
        Assertions.assertNotNull(response.getMethodResponses());
        Assertions.assertEquals(response.getMethodResponses().length, 2);
        MatcherAssert.assertThat(response.getMethodResponses()[1].getMethodResponse(), instanceOf(RequestTooLargeMethodErrorResponse.class));
    }

    @Test
    public void deserializeInvalidResponse() {
        final JsonParseException exception = Assertions.assertThrows(
                JsonParseException.class,
                () -> parseFromResource("response/invalid-response.json", GenericResponse.class)
        );
        Assertions.assertEquals("Unable to identify response as neither error nor response", exception.getMessage());
    }

    @Test
    public void deserializeInvalidResponseArray() {
        final JsonParseException exception = Assertions.assertThrows(
                JsonParseException.class,
                () -> parseFromResource("response/invalid-response-array.json", GenericResponse.class)
        );
        Assertions.assertEquals("unexpected json type when parsing response", exception.getMessage());
    }

    @Test
    public void deserializeResponseInvalidInvocationArray() {
        final JsonParseException exception = Assertions.assertThrows(
                JsonParseException.class,
                () -> parseFromResource("response/invalid-invocation-array.json", GenericResponse.class)
        );
        Assertions.assertEquals("Invocation array has 2 values. Expected 3", exception.getMessage());
    }

    @Test
    public void deserializeResponseInvalidInvocationNoName() {
        final JsonParseException exception = Assertions.assertThrows(
                JsonParseException.class,
                () -> parseFromResource("response/invalid-invocation-no-name.json", GenericResponse.class)
        );
        Assertions.assertEquals("Name (index 0 of JsonArray) must be a primitive string", exception.getMessage());
    }

    @Test
    public void deserializeResponseInvalidInvocationUnknownMethod() {
        final ResponseInvocationDeserializer.UnknownMethodNameException exception = Assertions.assertThrows(
                ResponseInvocationDeserializer.UnknownMethodNameException.class,
                () -> parseFromResource("response/invalid-invocation-unknown.json", GenericResponse.class)
        );
        Assertions.assertEquals("unknown/method", exception.getName());
    }

}
