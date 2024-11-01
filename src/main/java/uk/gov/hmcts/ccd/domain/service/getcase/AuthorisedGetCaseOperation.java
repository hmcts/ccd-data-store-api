package uk.gov.hmcts.ccd.domain.service.getcase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Slf4j
@Service
@Qualifier("authorised")
public class AuthorisedGetCaseOperation implements GetCaseOperation {
    private final GetCaseOperation getCaseOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final AccessControlService accessControlService;
    private final CaseDataAccessControl caseDataAccessControl;


    public AuthorisedGetCaseOperation(@Qualifier("classified") final GetCaseOperation getCaseOperation,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                      final CaseDefinitionRepository caseDefinitionRepository,
                                      final AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl) {
        this.getCaseOperation = getCaseOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.accessControlService = accessControlService;
        this.caseDataAccessControl =  caseDataAccessControl;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {

        return this.execute(caseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return getCaseOperation.execute(caseReference)
            .flatMap(caseDetails ->
                verifyReadAccess(getCaseType(caseDetails.getCaseTypeId()),
                    getAccessProfiles(caseReference),
                    caseDetails));
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        return caseDefinitionRepository.getCaseType(caseTypeId);
    }


    private Set<AccessProfile> getAccessProfiles(String caseReference) {
        return caseDataAccessControl.generateAccessProfilesByCaseReference(caseReference);
    }

    private Optional<CaseDetails> verifyReadAccess(CaseTypeDefinition caseType, Set<AccessProfile> accessProfiles,
                                                   CaseDetails caseDetails) {

        if (caseType == null || caseDetails == null || CollectionUtils.isEmpty(accessProfiles)) {
            return Optional.empty();
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType, accessProfiles, CAN_READ)
            || !accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(), caseType, accessProfiles,
            CAN_READ)) {
            log.info("Coming here with no read access");
            return Optional.empty();
        }

        caseDetails.setData(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                caseType.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                false)));
        caseDetails.setDataClassification(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                caseType.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                true)));
        log.info("Coming here for case details id {}", caseDetails.getId());

        return Optional.of(caseDetails);
    }

}
