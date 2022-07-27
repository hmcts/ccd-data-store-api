package uk.gov.hmcts.ccd.domain.service.getcase;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

public class AbstractRestrictCaseOperation {

    private final CaseDataAccessControl caseDataAccessControl;
    private final AccessControlService accessControlService;

    public AbstractRestrictCaseOperation(CaseDataAccessControl caseDataAccessControl,
                                         AccessControlService accessControlService) {
        this.caseDataAccessControl = caseDataAccessControl;
        this.accessControlService = accessControlService;
    }

    protected Set<AccessProfile> getAccessProfiles(String caseReference, CaseDetails caseDetails) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseReference, caseDetails);
    }

    protected boolean verifyCaseTypeReadAccess(CaseTypeDefinition caseTypeDefinition,
                                               Set<AccessProfile> accessProfiles) {
        return accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_READ);
    }
}
