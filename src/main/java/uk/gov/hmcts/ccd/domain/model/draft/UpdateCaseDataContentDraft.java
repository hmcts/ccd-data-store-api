package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class UpdateCaseDataContentDraft {

    public final CaseDataContentDraft document;

    @NotNull
    public final String type;

    // region constructor
    public UpdateCaseDataContentDraft(
        @JsonProperty("document") CaseDataContentDraft document,
        @JsonProperty("type") String type
    ) {
        this.document = document;
        this.type = type;
    }
    // endregion
}
