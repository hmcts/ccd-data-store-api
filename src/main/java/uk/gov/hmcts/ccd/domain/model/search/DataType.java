package uk.gov.hmcts.ccd.domain.model.search;

/**
 * Enum representing data types used in case data for the Field.
 */
public enum DataType {
    COLLECTION("collection"),
    COMPLEX("complex");

    private final String dataType;

    DataType(String dataType) {
        this.dataType = dataType;
    }
}
