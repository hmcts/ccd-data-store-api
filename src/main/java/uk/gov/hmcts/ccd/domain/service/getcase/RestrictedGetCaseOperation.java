package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.Optional;
import java.util.Set;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation extends AbstractRestrictCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final CaseTypeService caseTypeService;

    @Autowired
    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      final CaseDataAccessControl caseDataAccessControl,
                                      AccessControlService accessControlService,
                                      CaseTypeService caseTypeService) {
        super(caseDataAccessControl, accessControlService);
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.caseTypeService = caseTypeService;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return Optional.empty();
    }

    public Optional<CaseDetails> execute(String caseReference) {
        Optional<CaseDetails> caseDetails = defaultGetCaseOperation.execute(caseReference);

        if (caseDetails.isPresent()) {
            CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.get().getCaseTypeId());
            Set<AccessProfile> accessProfiles = getAccessProfiles(caseReference, caseDetails.get());
            if (!accessProfiles.isEmpty()) {
                if (!verifyCaseTypeReadAccess(caseTypeDefinition, accessProfiles)) {
                    throw new ForbiddenException();
                }
            }
        }
        throw new CaseNotFoundException(caseReference);
    }

    CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseTypeService.getCaseType(caseTypeId);
    }

}

