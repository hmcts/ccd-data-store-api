package uk.gov.hmcts.ccd.data.casedetails.search;

public abstract class Criterion {

    protected static final String TOKEN_SEPARATOR = ".";

    protected static final String POSITION_PREFIX = "?";

    private final String field;

    private final String soughtValue;

    Criterion(String field, String soughtValue) {
        this.field = field;
        this.soughtValue = soughtValue;
    }

    public String getField() {
        return field;
    }

    public String getSoughtValue() {
        return soughtValue;
    }

    public abstract String buildClauseString(int position, String operation);

    protected String makeCaseInsensitive(String in) {
        return "TRIM( UPPER ( " + in + "))";
    }

}
