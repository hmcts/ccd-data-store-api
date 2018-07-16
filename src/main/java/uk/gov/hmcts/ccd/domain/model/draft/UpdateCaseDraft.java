package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class UpdateCaseDraft {

    public final CaseDraft document;

    @NotNull
    public final String type;

    // region constructor
    public UpdateCaseDraft(
        @JsonProperty("document") CaseDraft document,
        @JsonProperty("type") String type
    ) {
        this.document = document;
        this.type = type;
    }
    // endregion
}
