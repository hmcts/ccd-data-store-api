package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;
import java.util.function.Predicate;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface GetCaseTypeOperation {
    Optional<CaseTypeDefinition> execute(String caseTypeId, Predicate<AccessControlList> access);
}
