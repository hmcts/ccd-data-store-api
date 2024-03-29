package uk.gov.hmcts.ccd.domain.model.common;

public enum DisplayContextParameterCollectionOptions {
    ALLOW_INSERT("allowInsert"),
    ALLOW_DELETE("allowDelete"),
    ALLOW_UPDATE("allowUpdate");

    private final String option;

    DisplayContextParameterCollectionOptions(String option) {
        this.option = option;
    }

    public String getOption() {
        return this.option;
    }
}
