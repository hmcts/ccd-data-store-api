package uk.gov.hmcts.ccd.domain.service.caseclosed;

import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;

import java.time.LocalDate;

public interface ClosedCaseSearchOperation {

    DateCaseClosedResponse execute(LocalDate closedCaseDate);
}
