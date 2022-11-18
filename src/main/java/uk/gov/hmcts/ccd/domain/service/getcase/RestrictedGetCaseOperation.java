package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final GetCaseOperation authorisedGetCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataAccessControl caseDataAccessControl;
    private final AccessControlService accessControlService;


    @Autowired
    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      @Qualifier("authorised") final GetCaseOperation authorisedGetCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
                                      final CaseDataAccessControl caseDataAccessControl,
                                      final AccessControlService accessControlService) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataAccessControl = caseDataAccessControl;
        this.accessControlService = accessControlService;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return this.execute(caseReference);
    }

    public Optional<CaseDetails> execute(String caseReference) {
        Optional<CaseDetails> authorisedCaseDetails = authorisedGetCaseOperation.execute(caseReference);

        if (authorisedCaseDetails.isEmpty()) {
            defaultGetCaseOperation.execute(caseReference)
                .ifPresent(caseDetails -> {
                    CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());
                    Set<AccessProfile> accessProfiles = getAccessProfiles(caseDetails);
                    if (hasReadAccess(caseTypeDefinition, accessProfiles, caseDetails)) {
                        throw new ForbiddenException();
                    }
                });
        }

        return authorisedCaseDetails;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }

    private Set<AccessProfile> getAccessProfiles(CaseDetails caseDetails) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
    }

    private boolean hasReadAccess(CaseTypeDefinition caseType,
                                  Set<AccessProfile> accessProfiles,
                                  CaseDetails caseDetails) {
        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(accessProfiles)) {
            return false;
        }

        return accessControlService.canAccessCaseTypeWithCriteria(caseType, accessProfiles, CAN_READ)
            && accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, accessProfiles,
            CAN_READ);
    }
}

