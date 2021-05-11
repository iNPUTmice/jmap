package rs.ltt.jmap.client;

public final class Version {
    public static final String ARTIFACT_ID = "${project.artifactId}";
    public static final String URL = "${parent.url}";
    public static final String VERSION = "${project.version}";

    private Version() {
        
    }

    public static String getUserAgent() {
        return String.format(
                "%s/%s (%s)",
                Version.ARTIFACT_ID, Version.VERSION, Version.URL
        );
    }
}
