package uk.gov.hmcts.ccd.data.definition;

import java.util.List;
import java.util.Optional;

import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

public interface CaseDefinitionRepository {
    List<CaseTypeDefinition> getCaseTypesForJurisdiction(String jurisdictionId);

    CaseTypeDefinition getCaseType(String caseTypeId);

    CaseTypeDefinition getCaseType(int version, String caseTypeId);

    List<FieldTypeDefinition> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    JurisdictionDefinition getJurisdiction(String jurisdictionId);

    Optional<List<String>> getAllCaseTypesByJurisdictions(List<String> jurisdictionIds);

}
