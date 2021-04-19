package uk.gov.hmcts.ccd.flyway;

public class PendingMigrationScriptException extends RuntimeException {

    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied " + script);
    }
}
