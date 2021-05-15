package uk.gov.hmcts.ccd.domain.service.search;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

@Service
public class CaseSearchesViewAccessControl {

    private final CaseTypeService caseTypeService;
    private final SearchResultDefinitionService searchResultDefinitionService;
    private final SecurityClassificationServiceImpl securityClassificationService;
    private final CaseDataAccessControl caseDataAccessControl;

    public CaseSearchesViewAccessControl(CaseTypeService caseTypeService,
                                         SearchResultDefinitionService searchResultDefinitionService,
                                         SecurityClassificationServiceImpl securityClassificationService,
                                         CaseDataAccessControl caseDataAccessControl) {
        this.caseTypeService = caseTypeService;
        this.searchResultDefinitionService = searchResultDefinitionService;
        this.securityClassificationService = securityClassificationService;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public Boolean filterResultsBySearchResultsDefinition(String useCase, CaseTypeDefinition caseTypeDefinition,
                                                          List<String> requestedFields, String caseFieldId) {
        Set<String> accessProfiles = getAccessProfiles(caseTypeDefinition.getId());
        SearchResultDefinition searchResultDefinition = searchResultDefinitionService
            .getSearchResultDefinition(caseTypeDefinition, useCase, requestedFields);

        if (useCase != null) {
            return searchResultDefinition.fieldExists(caseFieldId)
                && searchResultDefinition.fieldHasRole(caseFieldId, accessProfiles);
        }
        return true;
    }

    public Boolean filterFieldByAuthorisationAccessOnField(CaseFieldDefinition caseFieldDefinition) {
        if (!caseFieldDefinition.isMetadata()) {
            return getAccessProfiles(caseFieldDefinition.getCaseTypeId()).stream()
                .anyMatch(accessProfile ->
                    caseFieldDefinition.getAccessControlListByRole(accessProfile)
                        .map(AccessControlList::isRead).orElse(false));
        }
        return true;
    }

    public Boolean filterResultsBySecurityClassification(CaseFieldDefinition caseFieldDefinition,
                                                          CaseTypeDefinition caseTypeDefinition) {
        return securityClassificationService.userHasEnoughSecurityClassificationForField(
            caseTypeDefinition.getJurisdictionId(),
            caseTypeDefinition,
            caseFieldDefinition.getId());
    }

    private Set<String> getAccessProfiles(String caseTypeId) {
        List<AccessProfile> accessProfileList = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseTypeId);
        return caseDataAccessControl.extractAccessProfileNames(accessProfileList);
    }
}
