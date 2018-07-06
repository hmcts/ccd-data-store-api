package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ToString
public class CreateCaseDraft {

    public final CaseDraft document;

    @NotNull
    public final String type;

    @ApiModelProperty(
        name = "max_stale_days",
        notes = "Number of days before removing a draft that hasn't been updated"
    )
    @Min(value = 1L)
    public final Integer maxStaleDays;

    public CreateCaseDraft(
        @JsonProperty("document") CaseDraft document,
        @JsonProperty("type") String type,
        @JsonProperty("max_stale_days") Integer maxStaleDays
    ) {
        this.document = document;
        this.type = type;
        this.maxStaleDays = maxStaleDays;
    }
}
