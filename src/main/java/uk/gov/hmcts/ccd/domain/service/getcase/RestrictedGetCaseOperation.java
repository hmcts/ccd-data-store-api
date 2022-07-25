package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.util.Optional;
import java.util.Set;

@Service
@Qualifier("restricted")
public class RestrictedGetCaseOperation implements GetCaseOperation {

    private final GetCaseOperation defaultGetCaseOperation;
    private final CaseDataAccessControl caseDataAccessControl;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AuthorisedGetCaseOperation authorisedGetCaseOperation;

    @Autowired
    public RestrictedGetCaseOperation(@Qualifier("default") final GetCaseOperation defaultGetCaseOperation,
                                      CaseDataAccessControl caseDataAccessControl,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
                                      AuthorisedGetCaseOperation authorisedGetCaseOperation) {
        this.defaultGetCaseOperation = defaultGetCaseOperation;
        this.caseDataAccessControl = caseDataAccessControl;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.authorisedGetCaseOperation = authorisedGetCaseOperation;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return Optional.empty();
    }

    public Optional<CaseDetails> execute(String caseReference) {
        Optional<CaseDetails> caseDetailsFromDb = defaultGetCaseOperation.execute(caseReference);
        if (caseDetailsFromDb.isPresent()) {
            Optional<CaseDetails> restrictedCaseDetails = caseDetailsFromDb.flatMap(caseDetails ->
                authorisedGetCaseOperation.verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                    getAccessProfiles(caseReference),
                    caseDetails));
            if (!restrictedCaseDetails.isEmpty()) {
                throw new ForbiddenException();
            }
        }
        throw new CaseNotFoundException(caseReference);
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }

    private Set<AccessProfile> getAccessProfiles(String caseReference) {
        return caseDataAccessControl.generateAccessProfilesForRestrictedCase(caseReference);
    }

}

