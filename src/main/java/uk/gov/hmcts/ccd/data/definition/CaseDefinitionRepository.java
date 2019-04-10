package uk.gov.hmcts.ccd.data.definition;

import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;

public interface CaseDefinitionRepository {
    List<CaseType> getCaseTypesForJurisdiction(String jurisdictionId);

    List<String> getCaseTypesReferences();

    List<String> getCaseTypesReferences(HttpHeaders httpHeaders);

    CaseType getCaseType(String caseTypeId);

    CaseType getCaseType(String caseTypeId, HttpHeaders httpHeaders);

    CaseType getCaseType(int version, String caseTypeId);

    List<FieldType> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<UserRole> getClassificationsForUserRoleList(List<String> userRoles);

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);

    List<Jurisdiction> getJurisdictions(List<String> ids);
}
