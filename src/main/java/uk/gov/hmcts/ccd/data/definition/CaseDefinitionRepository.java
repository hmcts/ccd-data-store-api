package uk.gov.hmcts.ccd.data.definition;

import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;

public interface CaseDefinitionRepository {
    List<CaseTypeDefinition> getCaseTypesForJurisdiction(String jurisdictionId);

    CaseTypeDefinition getCaseType(String caseTypeId);

    CaseTypeDefinition getCaseType(int version, String caseTypeId);

    List<FieldTypeDefinition> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    JurisdictionDefinition getJurisdiction(String jurisdictionId);

    List<String> getCaseTypesIDsByJurisdictions(List<String> jurisdictionIds);

    List<String> getAllCaseTypesIDs();
}
