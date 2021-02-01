package uk.gov.hmcts.ccd.domain.types;

/**
 * Custom types IDs.
 */
public enum CustomTypes {

    CASE_LINK_TEXT_CASE_REFERENCE("TextCaseReference");

    private String id;

    CustomTypes(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
