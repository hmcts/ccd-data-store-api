package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaseLinkInfo {
    private String caseNameHmctsInternal;
    private String caseReference;
    private String ccdCaseType;
    private String ccdJurisdiction;
    private String state;
    private List<CaseLinkDetails> caseLinkDetails;
}
