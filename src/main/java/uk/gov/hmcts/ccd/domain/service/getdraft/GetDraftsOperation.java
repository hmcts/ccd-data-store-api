package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;

public interface GetDraftsOperation {
    /**
     * Execute.
     *
     * @param metadata a metadata containing jurisdiction and case type ids
     * @return A list of CaseDetails build from drafts matching jurisdiction and case type ids
     */
    List<CaseDetails> execute(MetaData metadata);
}
