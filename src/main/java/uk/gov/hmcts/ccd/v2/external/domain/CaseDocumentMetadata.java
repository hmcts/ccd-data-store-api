package uk.gov.hmcts.ccd.v2.external.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CaseDocumentMetadata {

    private String caseId;
    private String caseTypeId;
    private String jurisdictionId;
    private CaseDocument document;

}
