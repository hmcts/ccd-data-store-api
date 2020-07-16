package uk.gov.hmcts.ccd.domain.service.search;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import uk.gov.hmcts.ccd.data.user.*;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.*;

import java.util.*;

@Service
public class CaseSearchesViewAccessControl {

    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;
    private final SearchResultDefinitionService searchResultDefinitionService;
    private final SecurityClassificationService securityClassificationService;

    public CaseSearchesViewAccessControl(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                         CaseTypeService caseTypeService,
                                         SearchResultDefinitionService searchResultDefinitionService,
                                         SecurityClassificationService securityClassificationService) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
        this.searchResultDefinitionService = searchResultDefinitionService;
        this.securityClassificationService = securityClassificationService;
    }

    public Boolean filterResultsBySearchResultsDefinition(String useCase, String caseTypeId, List<String> requestedFields, String caseFieldId) {
        Set<String> roles = userRepository.getUserRoles();
        CaseTypeDefinition caseTypeDefinition = getCaseTypeDefinition(caseTypeId);
        SearchResultDefinition searchResultDefinition = searchResultDefinitionService.getSearchResultDefinition(caseTypeDefinition, useCase, requestedFields);

        if (useCase != null) {
            return searchResultDefinition.fieldExists(caseFieldId)
                && searchResultDefinition.fieldHasRole(caseFieldId, roles);
        }
        return true;
    }

    public Boolean filterFieldByAuthorisationAccessOnField(CaseFieldDefinition caseFieldDefinition) {
        if (!caseFieldDefinition.isMetadata()) {
            return userRepository.getUserRoles().stream()
                .anyMatch(role -> caseFieldDefinition.getAccessControlListByRole(role).map(AccessControlList::isRead).orElse(false));
        }
        return true;
    }

    public Boolean filterResultsBySecurityClassification(CaseFieldDefinition caseFieldDefinition,
                                                          CaseTypeDefinition caseTypeDefinition) {
        return securityClassificationService.userHasEnoughSecurityClassificationForField(caseTypeDefinition.getJurisdictionId(),
            caseTypeDefinition,
            caseFieldDefinition.getId());
    }

    private CaseTypeDefinition getCaseTypeDefinition(String caseTypeId) {
        return caseTypeService.getCaseType(caseTypeId);
    }
}
