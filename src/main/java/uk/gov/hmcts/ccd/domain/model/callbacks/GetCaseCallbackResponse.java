package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;

import java.util.List;

public class GetCaseCallbackResponse {

    @JsonProperty("metadataFields")
    private List<CaseViewField> metadataFields;

    public List<CaseViewField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(List<CaseViewField> metadataFields) {
        this.metadataFields = metadataFields;
    }
}
