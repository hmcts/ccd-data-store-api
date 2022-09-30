package uk.gov.hmcts.ccd.domain.model.std.validator;

public class ValidationError {

    public static final String ARGUMENT_INVALID = "Input not valid";
    public static final String SORT_BY_INVALID = "Sort by category invalid";
    public static final String SORT_DIRECTION_INVALID = "Sort direction invalid";
    public static final String MAX_RECORD_COUNT_INVALID = "Max return record count must be between 1 and 10000";
    public static final String GLOBAL_SEARCH_CRITERIA_INVALID =
        "At least one jurisdiction or case type must be provided in the search criteria";
    public static final String JURISDICTION_ID_LENGTH_INVALID = "Jurisdiction ID exceeds maximum length of 70";
    public static final String STATE_ID_LENGTH_INVALID = "State ID exceeds maximum length of 70";
    public static final String CASE_TYPE_ID_LENGTH_INVALID = "Case type ID exceeds maximum length of 70";
    public static final String CASE_REFERENCE_INVALID = "Case reference invalid format";
    public static final String DATE_OF_BIRTH_INVALID = "Date of birth invalid format";
    public static final String START_RECORD_NUMBER_INVALID = "Start record number must be 1 or more";
    public static final String DATE_OF_DEATH_INVALID = "Date of death invalid format";


    private ValidationError() {
    }
}
