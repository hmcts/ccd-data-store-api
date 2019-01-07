package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.function.Predicate;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

public interface GetCaseTypeOperation {
    Optional<CaseType> execute(String caseTypeId, Predicate<AccessControlList> access);
}
