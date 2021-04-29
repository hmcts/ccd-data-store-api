package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.AbstractCaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND_DETAILS;

public abstract class AbstractAuthorisedCaseViewOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseDataAccessControl caseDataAccessControl;

    AbstractAuthorisedCaseViewOperation(CaseDefinitionRepository caseDefinitionRepository,
                                        AccessControlService accessControlService,
                                        CaseDetailsRepository caseDetailsRepository,
                                        CaseDataAccessControl caseDataAccessControl) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    void verifyCaseTypeReadAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, userRoles, CAN_READ)) {
            ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(AccessControlService
                .NO_CASE_TYPE_FOUND);
            resourceNotFoundException.withDetails(NO_CASE_TYPE_FOUND_DETAILS);
            throw resourceNotFoundException;
        }
    }

    protected CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    protected String getCaseId(String caseReference) {
        return getCase(caseReference).getId();
    }

    protected CaseDetails getCase(String caseReference) {
        Optional<CaseDetails> caseDetails = this.caseDetailsRepository.findByReference(caseReference);
        return caseDetails
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    protected Set<String> getAccessProfiles(String caseReference) {
        List<AccessProfile> accessProfiles = caseDataAccessControl
            .generateAccessProfilesByCaseReference(caseReference);
        return caseDataAccessControl.extractAccessProfileNames(accessProfiles);
    }

    protected void filterAllowedTabsWithFields(AbstractCaseView abstractCaseView, Set<String> userRoles) {
        abstractCaseView.setTabs(Arrays.stream(abstractCaseView.getTabs())
            .filter(caseViewTab -> caseViewTab.getFields().length > 0 && tabAllowed(caseViewTab, userRoles))
            .toArray(CaseViewTab[]::new));
    }

    private boolean tabAllowed(final CaseViewTab caseViewTab, final Set<String> userRoles) {
        return StringUtils.isEmpty(caseViewTab.getRole()) || userRoles.contains(caseViewTab.getRole());
    }

    AccessControlService getAccessControlService() {
        return accessControlService;
    }
}
