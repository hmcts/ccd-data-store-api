package uk.gov.hmcts.ccd.domain.service.security;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface AuthorisedCaseDefinitionDataService {

    Optional<CaseTypeDefinition> getAuthorisedCaseType(String caseTypeId, Predicate<AccessControlList> access);

    List<CaseStateDefinition> getUserAuthorisedCaseStates(String jurisdiction, String caseTypeId,
                                                          Predicate<AccessControlList> access);

    List<String> getUserAuthorisedCaseStateIds(String jurisdiction, String caseTypeId,
                                               Predicate<AccessControlList> access);

    List<String> getUserAuthorisedCaseStateIds(String caseTypeId, Predicate<AccessControlList> access);

}
