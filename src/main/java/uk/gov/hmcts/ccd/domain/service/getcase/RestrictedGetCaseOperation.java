package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.Optional;
import java.util.Set;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      CaseDataAccessControl caseDataAccessControl) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return Optional.empty();
    }

    public Optional<CaseDetails> execute(String caseReference) {
        if (defaultGetCaseOperation.execute(caseReference).isPresent()) {
            Set<AccessProfile> accessProfiles = getAccessProfiles(caseReference);
            if (!accessProfiles.isEmpty()) {
                throw new ForbiddenException();
            }
        }
        throw new CaseNotFoundException(caseReference);
    }

    private Set<AccessProfile> getAccessProfiles(String caseReference) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseReference);
    }

}

