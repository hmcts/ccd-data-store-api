package uk.gov.hmcts.ccd.datastore.tests;

import org.apache.commons.lang3.Validate;

public class Env {

    private Env() {
    }

    public static String require(String name) {
        return Validate.notNull(System.getenv(name), "Environment variable `%s` is required", name);
    }
}
