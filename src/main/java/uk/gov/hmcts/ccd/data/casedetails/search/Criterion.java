package uk.gov.hmcts.ccd.data.casedetails.search;

public abstract class Criterion {

    protected static final String TOKEN_SEPARATOR = ".";

    protected static final String PARAM_PREFIX = ":";

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

    public String buildParameterId() {
        return field.replaceAll("[()]", "_");
    }

    public abstract String buildClauseString(String operation);

    protected String makeCaseInsensitive(String in) {
        return "TRIM( UPPER ( " + in + "))";
    }

}
