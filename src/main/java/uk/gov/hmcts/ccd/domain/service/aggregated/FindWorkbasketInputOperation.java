package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import java.util.function.Predicate;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

public interface FindWorkbasketInputOperation {
    List<WorkbasketInput> execute(final String caseTypeId, Predicate<AccessControlList> access);
}
