package uk.gov.hmcts.ccd.domain.service.startevent;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Slf4j
@Service
@Qualifier("authorised")
public class AuthorisedStartEventOperation implements StartEventOperation {

    private final StartEventOperation startEventOperation;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final AccessControlService accessControlService;
    private final UIDService uidService;
    private final CaseAccessService caseAccessService;
    private final DraftGateway draftGateway;

    public AuthorisedStartEventOperation(@Qualifier("classified") final StartEventOperation startEventOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                         final CaseDefinitionRepository caseDefinitionRepository,
                                         @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                         final CaseDetailsRepository caseDetailsRepository,
                                         final AccessControlService accessControlService,
                                         final UIDService uidService,
                                         @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                         CaseAccessService caseAccessService) {

        this.startEventOperation = startEventOperation;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.accessControlService = accessControlService;
        this.uidService = uidService;
        this.caseAccessService = caseAccessService;
        this.draftGateway = draftGateway;
    }

    @Override
    public StartEventResult triggerStartForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                        eventId,
                                                                                        ignoreWarning));
    }

    @Override
    public StartEventResult triggerStartForCase(String caseReference, String eventId,
        Boolean ignoreWarning) {

        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        Optional<CaseDetails> caseDetailsOptional = caseDetailsRepository.findByReference(
            caseReference);
        if (caseDetailsOptional.isEmpty()) {
            throw new CaseNotFoundException(caseReference);
        }

        return caseDetailsOptional.map(
                caseDetails -> verifyReadAccess(caseDetails.getCaseTypeId(),
                    startEventOperation.triggerStartForCase(caseReference, eventId, ignoreWarning)))
            .orElseThrow(() -> {
                log.error("event={} could not be started for case={} in state={}",
                    eventId, caseReference, caseDetailsOptional.get().getState());
                return new ValidationException(caseReference);
            });
    }

    @Override
    public StartEventResult triggerStartForDraft(String draftReference,
                                                 Boolean ignoreWarning) {

        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForDraft(draftReference,
            ignoreWarning));
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    private Set<AccessProfile> getCaseRoles(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        if (caseDetails == null || caseDetails.getId() == null || Draft.isDraft(caseDetails.getId())) {
            return Sets.union(caseAccessService.getCreationAccessProfiles(caseTypeDefinition.getId()),
                caseAccessService.getCaseCreationCaseRoles());
        } else {
            return caseAccessService.getAccessProfilesByCaseReference(caseDetails.getReferenceAsString());
        }
    }

    private StartEventResult verifyReadAccess(final String caseTypeId, final StartEventResult startEventResult) {

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);

        CaseDetails caseDetails = startEventResult.getCaseDetails();
        Set<AccessProfile> caseAccessProfiles = getCaseRoles(caseDetails, caseTypeDefinition);

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            caseAccessProfiles,
            CAN_READ)) {
            caseDetails.setData(newHashMap());
            caseDetails.setDataClassification(newHashMap());
            return startEventResult;
        }

        if (caseDetails != null) {
            caseDetails.setData(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    caseAccessProfiles,
                    CAN_READ,
                    false)));
            caseDetails.setDataClassification(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                    caseTypeDefinition.getCaseFieldDefinitions(),
                    caseAccessProfiles,
                    CAN_READ,
                    true)));
        }
        return startEventResult;
    }


}
