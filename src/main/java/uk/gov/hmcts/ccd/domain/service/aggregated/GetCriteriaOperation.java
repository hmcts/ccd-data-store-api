package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;

import java.util.List;
import java.util.function.Predicate;

public interface GetCriteriaOperation {

    <T> List<? extends CriteriaInput> execute(
        final String caseTypeId,
        Predicate<AccessControlList> access, CriteriaType criteriaType);
}
