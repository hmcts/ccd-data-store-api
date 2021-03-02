package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.List;
import java.util.function.Predicate;

public interface GetCaseTypesOperation {
    List<CaseTypeDefinition> execute(String jurisdictionId, Predicate<AccessControlList> access);
}
