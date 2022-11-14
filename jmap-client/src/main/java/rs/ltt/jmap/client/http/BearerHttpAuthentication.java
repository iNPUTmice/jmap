package rs.ltt.jmap.client.http;

import okhttp3.Request;

public class BearerHttpAuthentication implements HttpAuthentication {

    private final String username;
    private final String token;

    public BearerHttpAuthentication(String username, String token) {
        this.username = username;
        this.token = token;
    }

    @Override
    public void authenticate(Request.Builder builder) {
        builder.header("Authorization", "Bearer " + token);
    }

    @Override
    public String getUsername() {
        return username;
    }
}
