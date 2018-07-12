package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.List;

public interface GetDraftsOperation {
    /**
     *
     * @param metadata a metadata containing jurisdiction and case type ids
     * @return A list of CaseDetails matching jurisdiction and case type ids
     *
     */

    List<CaseDetails> execute(MetaData metadata);
}
