package uk.gov.hmcts.ccd.datastore.tests;

public class Env {

    private Env() {}

    public static String require(String name) {
        final String value = System.getenv(name);
        if (null == value) {
            throw new IllegalStateException(String.format("Environment variable `%s` is required", name));
        }
        return value;
    }
}
