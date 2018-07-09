package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.Draft;

import java.util.List;

public interface GetDraftsOperation {
    List<Draft> execute();
}
