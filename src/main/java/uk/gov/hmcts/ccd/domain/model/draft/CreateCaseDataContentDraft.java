package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ToString
public class CreateCaseDataContentDraft {

    public final CaseDataContentDraft document;

    @NotNull
    public final String type;

    @ApiModelProperty(
        name = "max_stale_days",
        notes = "Number of days before removing a draft that hasn't been updated"
    )
    @Min(value = 1L)
    public final Integer maxStaleDays;

    public CreateCaseDataContentDraft(
        @JsonProperty("document") CaseDataContentDraft document,
        @JsonProperty("type") String type,
        @JsonProperty("max_age") Integer maxStaleDays
    ) {
        this.document = document;
        this.type = type;
        this.maxStaleDays = maxStaleDays;
    }
}
