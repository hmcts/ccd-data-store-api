package uk.gov.hmcts.ccd.domain.model.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

/**
 * CaseDocumentMetadata.
 */

@Data
@Builder
public class CaseDocumentsMetadata {

    public CaseDocumentsMetadata(String caseId,
                                 String caseTypeId,
                                 String jurisdictionId,
                                 List<DocumentHashToken> documentHashTokens) {
        this.caseId = caseId;
        this.caseTypeId = caseTypeId;
        this.jurisdictionId = jurisdictionId;
        this.documentHashTokens = documentHashTokens;
    }

    @JsonProperty
    private String caseId;

    @JsonProperty
    private String caseTypeId;

    @JsonProperty
    private String jurisdictionId;

    @JsonProperty("documentHashTokens")
    private List<DocumentHashToken> documentHashTokens;

}
