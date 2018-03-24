package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

import java.util.List;
import java.util.function.Predicate;

public interface FindWorkbasketInputOperation {
    List<WorkbasketInput> execute(final String jurisdictionId, final String caseTypeId, Predicate<AccessControlList> access);
}
