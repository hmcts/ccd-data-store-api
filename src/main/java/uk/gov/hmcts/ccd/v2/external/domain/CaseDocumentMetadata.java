package uk.gov.hmcts.ccd.v2.external.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CaseDocumentMetadata {

    private String caseId;
    private DocumentPermissions documentPermissions;

}
