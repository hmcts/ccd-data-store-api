package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum CatalogueResponseCode {

    // CCD.00 Success codes

    // CCD.01 Validation codes
    VALIDATION_INVALID_DATA(CatalogueResponseGroup.VALIDATION, "01", "Invalid data provided."),

    // CCD.02 Callback codes
    CALLBACK_FAILURE(CatalogueResponseGroup.CALLBACK, "01", "Callback failure."),
    CALLBACK_PAYMENT_REQUIRED(CatalogueResponseGroup.CALLBACK, "02", "Payment required by callback.");

    final CatalogueResponseGroup group;
    final String code;
    final String message;

    CatalogueResponseCode(final CatalogueResponseGroup group, final String code, final String message) {
        this.group = group;
        this.code = code;
        this.message = message;
    }

    @JsonIgnore
    public CatalogueResponseGroup getGroup() {
        return group;
    }

    public String getCode() {
        return String.format("%s.%s", group.getCode(), code);
    }

    public String getMessage() {
        return message;
    }

}
