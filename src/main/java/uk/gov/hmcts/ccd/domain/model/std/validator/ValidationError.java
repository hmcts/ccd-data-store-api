package uk.gov.hmcts.ccd.domain.model.std.validator;

public class ValidationError {

    public static final String ARGUMENT_INVALID = "Input not valid";
    public static final String PARTIES_INVALID = "At least two fields must be defined for a party";
    public static final String SORT_BY_INVALID = "Sort by category invalid";
    public static final String SORT_DIRECTION_INVALID = "Sort direction invalid";
    public static final String MAX_RECORD_COUNT_INVALID = "Max return record count invalid";
    public static final String SEARCH_CRITERIA_MISSING = "Must have at least one search criteria";
    public static final String JURISDICTION_ID_LENGTH_INVALID = "Jurisdiction ID exceeds maximum length";
    public static final String STATE_ID_LENGTH_INVALID = "State ID exceeds maximum length";
    public static final String CASE_TYPE_ID_LENGTH_INVALID = "Case type ID exceeds maximum length";
    public static final String CASE_REFERENCE_INVALID = "Case reference invalid format";
    public static final String POSTCODE_INVALID = "Postcode invalid format";
    public static final String DATE_OF_BIRTH_INVALID = "Date of birth invalid format";

    private ValidationError() {
    }
}
