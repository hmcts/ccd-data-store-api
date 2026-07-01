package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ToString
public class CreateCaseDraftRequest {

    private CaseDraft document;

    @NotNull
    private String type;

    @Schema(
        name = "max_stale_days",
        description = "Number of days before removing a draft that hasn't been updated"
    )
    @Min(value = 1L)
    @JsonProperty("max_age")
    private Integer maxTTLDays;

    public CaseDraft getDocument() {
        return document;
    }

    public String getType() {
        return type;
    }

    public Integer getMaxTTLDays() {
        return maxTTLDays;
    }

    public void setDocument(CaseDraft document) {
        this.document = document;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMaxTTLDays(Integer maxTTLDays) {
        this.maxTTLDays = maxTTLDays;
    }
}
