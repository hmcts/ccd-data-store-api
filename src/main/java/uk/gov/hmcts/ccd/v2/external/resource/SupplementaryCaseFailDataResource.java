package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryCaseFailDataResource {

    @JsonProperty("caseId")
    private String caseId;

    @JsonProperty("failure_reason")
    private String failureMessage;

    public SupplementaryCaseFailDataResource(final String caseId, final String reason) {
        this.caseId = caseId;
        this.failureMessage = reason;
    }
}
