package uk.gov.hmcts.ccd.data.definition;

import org.springframework.scheduling.annotation.Async;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CaseDefinitionRepository {
    List<CaseType> getCaseTypesForJurisdiction(String jurisdictionId);

    CaseType getCaseType(String caseTypeId);

    CaseType getCaseType(int version, String caseTypeId);

    List<FieldType> getBaseTypes();

    UserRole getUserRoleClassifications(String userRole);

    List<Jurisdiction> getJurisdictions(List<String> ids);

    CompletableFuture<List<Jurisdiction>> getAllJurisdictionsAsync();

    CaseTypeDefinitionVersion getLatestVersion(String caseTypeId);
}
