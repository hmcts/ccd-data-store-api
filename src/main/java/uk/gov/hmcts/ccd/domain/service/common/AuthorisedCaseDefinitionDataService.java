package uk.gov.hmcts.ccd.domain.service.common;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;

import java.util.List;
import java.util.function.Predicate;

public interface AuthorisedCaseDefinitionDataService {

    List<CaseState> getUserAuthorisedCaseStates(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access);

    List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId, Predicate<AccessControlList> access);

}
