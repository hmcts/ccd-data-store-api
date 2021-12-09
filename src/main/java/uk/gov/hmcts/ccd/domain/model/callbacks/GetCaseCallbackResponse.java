package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;

import java.util.List;

@Data
@NoArgsConstructor
public class GetCaseCallbackResponse {

    @JsonProperty("metadataFields")
    private List<CaseViewField> metadataFields;
}
