package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

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
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchCaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

@Service
@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER)
public class AuthorisedCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "AuthorisedCaseSearchOperation";

    private final CaseSearchOperation caseSearchOperation;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AccessControlService accessControlService;
    private final SecurityClassificationService classificationService;
    private final ObjectMapperService objectMapperService;
    private final UserRepository userRepository;

    @Autowired
    public AuthorisedCaseSearchOperation(
        @Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
            AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
            AccessControlService accessControlService,
            SecurityClassificationService classificationService,
            ObjectMapperService objectMapperService,
            @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {

        this.caseSearchOperation = caseSearchOperation;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.accessControlService = accessControlService;
        this.classificationService = classificationService;
        this.objectMapperService = objectMapperService;
        this.userRepository = userRepository;
    }

    @Override
    public CaseSearchResult execute(CaseSearchRequest caseSearchRequest) {
        return authorisedCaseDefinitionDataService
            .getAuthorisedCaseType(caseSearchRequest.getCaseTypeId(), CAN_READ)
            .map(caseType -> searchCasesAndFilterFieldsByAccess(caseType, caseSearchRequest))
            .orElse(CaseSearchResult.EMPTY);
    }

    private CaseSearchResult searchCasesAndFilterFieldsByAccess(CaseType caseType, CaseSearchRequest caseSearchRequest) {
        CaseSearchResult result = caseSearchOperation.execute(caseSearchRequest);
        filterFieldsByAccess(caseType, result.getCases());
        return result;
    }

    private void filterFieldsByAccess(CaseType caseType, List<CaseDetails> cases) {
        cases.forEach(caseDetails -> {
            filterCaseFieldsByAclAccess(caseType, caseDetails);
            filterCaseFieldsBySecurityClassification(caseDetails);
        });
    }

    private void filterCaseFieldsByAclAccess(CaseType caseType, CaseDetails caseDetails) {
        JsonNode data = objectMapperService.convertObjectToJsonNode(caseDetails.getData());
        JsonNode filteredData = accessControlService.filterCaseFieldsByAccess(data, caseType.getCaseFields(), getUserRoles(), CAN_READ);
        caseDetails.setData(objectMapperService.convertJsonNodeToMap(filteredData));
    }

    private void filterCaseFieldsBySecurityClassification(CaseDetails caseDetails) {
        classificationService.applyClassification(caseDetails);
    }

    private Set<String> getUserRoles() {
        return userRepository.getUserRoles();
    }
}
