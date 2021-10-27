package rs.ltt.jmap.gson;

import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rs.ltt.jmap.common.ErrorResponse;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.entity.ErrorType;

public class ErrorResponseDeserializationTest extends AbstractGsonTest {

    @Test
    public void deserializeUnknownCapability() throws IOException {
        GenericResponse genericResponse =
                parseFromResource("response-error/unknown-capability.json", GenericResponse.class);
        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));
        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(errorResponse.getType(), ErrorType.UNKNOWN_CAPABILITY);
    }

    @Test
    public void deserializeNotJson() throws IOException {
        GenericResponse genericResponse =
                parseFromResource("response-error/not-json.json", GenericResponse.class);
        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));
        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(errorResponse.getType(), ErrorType.NOT_JSON);
    }

    @Test
    public void deserializeNotRequest() throws IOException {
        GenericResponse genericResponse =
                parseFromResource("response-error/not-request.json", GenericResponse.class);
        MatcherAssert.assertThat(genericResponse, CoreMatchers.instanceOf(ErrorResponse.class));
        ErrorResponse errorResponse = (ErrorResponse) genericResponse;
        Assertions.assertEquals(errorResponse.getType(), ErrorType.NOT_REQUEST);
    }
}
