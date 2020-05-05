package uk.gov.hmcts.ccd.domain.service.startevent;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

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
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
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
    public StartEventTrigger triggerStartForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        return verifyReadAccess(caseTypeId, startEventOperation.triggerStartForCaseType(caseTypeId,
            eventTriggerId,
            ignoreWarning));
    }

    @Override
    public StartEventTrigger triggerStartForCase(String caseReference, String eventTriggerId, Boolean ignoreWarning) {

        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference)
            .map(caseDetails -> verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForCase(caseReference,
                eventTriggerId,
                ignoreWarning)))
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    @Override
    public StartEventTrigger triggerStartForDraft(String draftReference,
                                                  Boolean ignoreWarning) {

        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        return verifyReadAccess(caseDetails.getCaseTypeId(), startEventOperation.triggerStartForDraft(draftReference,
            ignoreWarning));
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    private Set<String> getCaseRoles(CaseDetails caseDetails) {
        if (caseDetails == null || caseDetails.getId() == null || Draft.isDraft(caseDetails.getId())) {
            return caseAccessService.getCaseCreationCaseRoles();
        } else {
            return caseAccessService.getCaseRoles(caseDetails.getId());
        }
    }

    private StartEventTrigger verifyReadAccess(final String caseTypeId, final StartEventTrigger startEventTrigger) {

        final CaseType caseType = getCaseType(caseTypeId);

        Set<String> userRoles = Sets.union(caseAccessService.getUserRoles(), getCaseRoles(startEventTrigger.getCaseDetails()));

        CaseDetails caseDetails = startEventTrigger.getCaseDetails();

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseType,
            userRoles,
            CAN_READ)) {
            caseDetails.setData(newHashMap());
            caseDetails.setDataClassification(newHashMap());
            return startEventTrigger;
        }

        if (caseDetails != null) {
            caseDetails.setData(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ,
                    false)));
            caseDetails.setDataClassification(JacksonUtils.convertValue(
                accessControlService.filterCaseFieldsByAccess(
                    JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                    caseType.getCaseFields(),
                    userRoles,
                    CAN_READ,
                    true)));
        }
        return startEventTrigger;
    }


}
