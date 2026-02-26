package uk.gov.hmcts.ccd.domain.model.search;

/**
 * Enum representing data types used in case data for the Field.
 */
public enum DataType {
    COLLECTION("collection"),
    COMPLEX("complex");

    private final String fieldType;

    DataType(String fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public String toString() {
        return this.fieldType;
    }
}
