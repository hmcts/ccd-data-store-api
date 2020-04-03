package uk.gov.hmcts.ccd.domain.model.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;

/**
 * CaseDocumentMetadata.
 */

@Data
@Builder
public class DocumentMetadata {
    public DocumentMetadata() {
    }

    public DocumentMetadata(String caseId, String caseTypeId, String jurisdictionId, List<CaseDocument> documents) {
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.jurisdictionId = jurisdictionId;
        this.documents = documents;
    }

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty
    private List<CaseDocument> documents;

}
