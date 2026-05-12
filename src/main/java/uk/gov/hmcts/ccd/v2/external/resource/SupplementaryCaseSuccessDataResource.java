package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

import java.util.Map;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryCaseSuccessDataResource {

    @JsonProperty("caseId")
    private String caseId;

    @JsonProperty("supplementary_data")
    private Map<String, Object> response;

    public SupplementaryCaseSuccessDataResource(final String caseId, final SupplementaryData supplementaryDataUpdated) {
        this.caseId = caseId;
        this.response = supplementaryDataUpdated.getResponse();
    }
}
