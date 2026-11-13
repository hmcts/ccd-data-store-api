package uk.gov.hmcts.ccd.error;

/** Shared fixed validation messages used by producers and the public-message allowlist. */
public final class ValidationMessages {
    public static final String CASE_REFERENCE_INVALID = "Case reference is not valid";
    public static final String CASE_ID_INVALID = "Case ID is not valid";
    public static final String CASE_TYPE_INVALID = "Case Type Id is not valid";
    public static final String START_TRIGGER_TOKEN_MISSING = "Missing start trigger token";
    public static final String START_TRIGGER_TOKEN_EXPIRED =
        "Start trigger token has expired. Please restart the event.";
    public static final String ACCESS_INVALID = "Access can only be 'create', 'read' or 'update'";
    public static final String SORT_ORDER_INVALID = "Sort order field is invalid.";
    public static final String CASE_VERSION_INVALID = "003 Wrong CaseVersion";
    public static final String CATEGORY_ID_INVALID = "002 Invalid categoryId";
    public static final String EVENT_STATE_INVALID = "The case status did not qualify for the event";
    public static final String CASE_DATA_INVALID = "Case data validation failed";

    private ValidationMessages() {
    }
}
