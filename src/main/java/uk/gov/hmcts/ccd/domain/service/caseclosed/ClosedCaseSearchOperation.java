package uk.gov.hmcts.ccd.domain.service.caseclosed;

import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;

import java.util.Date;

public interface ClosedCaseSearchOperation {

    DateCaseClosedResponse execute(Date closedCaseDate);
}
