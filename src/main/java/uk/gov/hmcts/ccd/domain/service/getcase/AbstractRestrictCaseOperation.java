package uk.gov.hmcts.ccd.domain.service.getcase;

import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

public class AbstractRestrictCaseOperation {

    private final CaseDataAccessControl caseDataAccessControl;
    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public AbstractRestrictCaseOperation(CaseDefinitionRepository caseDefinitionRepository,
                                         CaseDataAccessControl caseDataAccessControl,
                                         AccessControlService accessControlService) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataAccessControl = caseDataAccessControl;
        this.accessControlService = accessControlService;
    }

    protected CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    protected Set<AccessProfile> getAccessProfiles(String caseReference, CaseDetails caseDetails) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseReference, caseDetails);
    }

    boolean verifyCaseTypeReadAccess(CaseTypeDefinition caseTypeDefinition, Set<AccessProfile> accessProfiles) {
        return accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ);
    }
}
