package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseLinkInfo {
    private String caseNameHmctsInternal;
    private String caseReference;
    private String ccdCaseType;
    private String ccdJurisdiction;
    private String state;
    private List<CaseLinkDetails> linkDetails;
}
