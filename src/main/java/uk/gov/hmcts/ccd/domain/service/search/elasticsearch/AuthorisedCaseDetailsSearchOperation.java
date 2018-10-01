package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Service
@Qualifier(AuthorisedCaseDetailsSearchOperation.QUALIFIER)
public class AuthorisedCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

    public static final String QUALIFIER = "AuthorisedCaseDetailsSearchOperation";

    private final CaseDetailsSearchOperation caseDetailsSearchOperation;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AccessControlService accessControlService;
    private final SecurityClassificationService classificationService;
    private final ObjectMapperService objectMapperService;
    private final UserRepository userRepository;

    @Autowired
    public AuthorisedCaseDetailsSearchOperation(
            @Qualifier(ElasticsearchCaseDetailsSearchOperation.QUALIFIER) CaseDetailsSearchOperation caseDetailsSearchOperation,
            AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
            AccessControlService accessControlService,
            SecurityClassificationService classificationService,
            ObjectMapperService objectMapperService,
            @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {

        this.caseDetailsSearchOperation = caseDetailsSearchOperation;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.accessControlService = accessControlService;
        this.classificationService = classificationService;
        this.objectMapperService = objectMapperService;
        this.userRepository = userRepository;
    }

    @Override
    public CaseDetailsSearchResult execute(String caseTypeId, String jsonQuery) {
        return authorisedCaseDefinitionDataService
            .getAuthorisedCaseType(caseTypeId, CAN_READ)
            .map(caseType -> {
                CaseDetailsSearchResult result = search(caseType, jsonQuery);
                filterFieldsByAccess(caseType, result.getCases());
                return result;
            })
            .orElse(CaseDetailsSearchResult.EMPTY);
    }

    private CaseDetailsSearchResult search(CaseType caseType, String jsonQuery) {
        return caseDetailsSearchOperation.execute(caseType.getId(), jsonQuery);
    }

    private void filterFieldsByAccess(CaseType caseType, List<CaseDetails> cases) {
        cases.forEach(caseDetails -> {
            filterCaseFieldsByAclAccess(caseType, caseDetails);
            filterCaseFieldsBySecurityClassification(caseDetails);
        });
    }

    private void filterCaseFieldsByAclAccess(CaseType caseType, CaseDetails caseDetails) {
        JsonNode data = objectMapperService.convertObjectToJsonNode(caseDetails.getData());
        JsonNode filteredData = accessControlService.filterCaseFieldsByAccess(data, caseType.getCaseFields(), getUserRoles(), AccessControlService.CAN_READ);
        caseDetails.setData(objectMapperService.convertJsonNodeToMap(filteredData));
    }

    private void filterCaseFieldsBySecurityClassification(CaseDetails caseDetails) {
        classificationService.applyClassification(caseDetails);
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }
}
