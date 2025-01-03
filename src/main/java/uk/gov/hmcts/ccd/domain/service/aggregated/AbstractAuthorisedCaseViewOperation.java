package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.AbstractCaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
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

    void verifyCaseTypeReadAccess(CaseTypeDefinition caseTypeDefinition, Set<AccessProfile> accessProfiles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ)) {
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

    protected Set<AccessProfile> getAccessProfiles(String caseReference) {
        return caseDataAccessControl
            .generateAccessProfilesByCaseReference(caseReference);
    }

    protected void filterAllowedTabsWithFields(AbstractCaseView abstractCaseView, Set<AccessProfile> accessProfiles) {
        abstractCaseView.setTabs(Arrays.stream(abstractCaseView.getTabs())
            .filter(caseViewTab -> caseViewTab.getFields().length > 0 && tabAllowed(caseViewTab, accessProfiles))
            .toArray(CaseViewTab[]::new));
    }

    private boolean tabAllowed(final CaseViewTab caseViewTab, final Set<AccessProfile> accessProfiles) {
        Set<String> accessProfileNames = AccessControlService.extractAccessProfileNames(accessProfiles);
        return StringUtils.isEmpty(caseViewTab.getRole()) || accessProfileNames.contains(caseViewTab.getRole());
    }

    protected void filterCaseTabFieldsByReadAccess(AbstractCaseView caseHistoryView,
                                                   Set<AccessProfile> accessProfiles) {
        caseHistoryView.setTabs(Arrays.stream(caseHistoryView.getTabs()).map(
            caseViewTab -> {
                caseViewTab.setFields(Arrays.stream(caseViewTab.getFields())
                    .filter(caseViewField -> getAccessControlService()
                        .canAccessCaseViewFieldWithCriteria(caseViewField, accessProfiles, CAN_READ))
                    .toArray(CaseViewField[]::new));
                return caseViewTab;
            }).toArray(CaseViewTab[]::new));
    }

    AccessControlService getAccessControlService() {
        return accessControlService;
    }
}
