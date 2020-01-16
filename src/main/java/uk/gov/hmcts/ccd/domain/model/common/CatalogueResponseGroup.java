package uk.gov.hmcts.ccd.domain.model.common;

public enum CatalogueResponseGroup {

    SUCCESS("00"),
    VALIDATION("01"),
    CALLBACK("02");

    String code;

    CatalogueResponseGroup(String code) {
        this.code = code;
    }

    public String getCode() {
        return String.format("CCD.%s", code);
    }

}
