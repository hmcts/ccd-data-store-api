package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum CatalogueResponseElement {

    // CCD.00 Success codes

    // CCD.01 Validation codes
    VALIDATION_INVALID_DATA(CatalogueResponseGroup.VALIDATION, 1, "Invalid data provided."),

    // CCD.02 Callback codes
    CALLBACK_FAILURE(CatalogueResponseGroup.CALLBACK, 1, "Callback failure."),
    CALLBACK_PAYMENT_REQUIRED(CatalogueResponseGroup.CALLBACK, 2, "Payment required by callback."),
    CALLBACK_BAD_ASSERT_FOR_UPSTREAM(CatalogueResponseGroup.CALLBACK, 3, "Callback response contained a bad AssertForUpstream request");

    final CatalogueResponseGroup group;
    final int sequenceNo;
    final String message;

    CatalogueResponseElement(final CatalogueResponseGroup group, final int sequenceNo, final String message) {
        this.group = group;
        this.sequenceNo = sequenceNo;
        this.message = message;
    }

    @JsonIgnore
    public CatalogueResponseGroup getGroup() {
        return group;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public String getCode() {
        return String.format("%s.%02d", getGroup().getCode(), getSequenceNo());
    }

    public String getMessage() {
        return message;
    }

}
