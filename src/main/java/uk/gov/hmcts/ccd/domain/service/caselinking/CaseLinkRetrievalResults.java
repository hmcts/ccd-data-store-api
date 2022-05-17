package uk.gov.hmcts.ccd.domain.service.caselinking;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;

@Builder
@Data
public class CaseLinkRetrievalResults {
    private List<CaseDetails> caseDetails;
    private boolean hasMoreResults;
}
