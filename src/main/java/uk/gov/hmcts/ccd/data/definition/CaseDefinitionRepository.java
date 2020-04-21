package uk.gov.hmcts.ccd.data.definition;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

public interface CaseDefinitionRepository {
    List<CaseTypeDefinition> getCaseTypesForJurisdiction(String jurisdictionId);

    CaseTypeDefinition getCaseType(String caseTypeId);

    CaseTypeDefinition getCaseType(int version, String caseTypeId);

    List<FieldType> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    JurisdictionDefinition getJurisdiction(String jurisdictionId);

}
