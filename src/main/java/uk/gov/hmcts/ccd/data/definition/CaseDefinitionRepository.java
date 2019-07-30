package uk.gov.hmcts.ccd.data.definition;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

public interface CaseDefinitionRepository {
    List<CaseType> getCaseTypesForJurisdiction(String jurisdictionId);

    CaseType getCaseType(String caseTypeId);

    CaseType getCaseType(int version, String caseTypeId);

    List<FieldType> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    Jurisdiction getJurisdiction(String jurisdictionId);
    
}
