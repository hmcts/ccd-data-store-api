package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@Qualifier("default")
public class DefaultGetEventTriggerOperation implements GetEventTriggerOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseViewFieldBuilder caseViewFieldBuilder;
    private final StartEventOperation startEventOperation;
    private final UIDService uidService;
    private final DraftGateway draftGateway;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Autowired
    public DefaultGetEventTriggerOperation(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                           @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                           final EventTriggerService eventTriggerService,
                                           final CaseViewFieldBuilder caseViewFieldBuilder,
                                           final UIDefinitionRepository uiDefinitionRepository,
                                           final UIDService uidService,
                                           @Qualifier("authorised") final StartEventOperation startEventOperation,
                                           final DraftGateway draftGateway,
                                           final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseViewFieldBuilder = caseViewFieldBuilder;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.uidService = uidService;
        this.startEventOperation = startEventOperation;
        this.draftGateway = draftGateway;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public CaseEventTrigger executeForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                          eventTriggerId,
                                                                                          ignoreWarning);
        return merge(startEventTrigger,
                     caseTypeId,
                     eventTriggerId,
                     null);
    }

    @Override
    public CaseEventTrigger executeForCase(String caseReference,
                                           String eventTriggerId,
                                           Boolean ignoreWarning) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);

        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForCase(caseReference,
                                                                                      eventTriggerId,
                                                                                      ignoreWarning);
        return merge(startEventTrigger,
                     caseDetails.getCaseTypeId(),
                     eventTriggerId,
                     caseReference);
    }

    @Override
    public CaseEventTrigger executeForDraft(String draftReference,
                                            String eventTriggerId,
                                            Boolean ignoreWarning) {
        final CaseDetails caseDetails = getDraftDetails(draftReference);

        StartEventTrigger startEventTrigger = startEventOperation.triggerStartForDraft(draftReference,
                                                                                       eventTriggerId,
                                                                                       ignoreWarning);
        return merge(startEventTrigger,
                     caseDetails.getCaseTypeId(),
                     eventTriggerId,
                     draftReference);
    }

    private CaseDetails getDraftDetails(String draftId) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftId));
        return draftResponseToCaseDetailsBuilder.build(draftResponse);
    }

    private CaseDetails getCaseDetails(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference).orElseThrow(
            () -> new CaseNotFoundException(caseReference));
    }

    private CaseEventTrigger buildCaseEventTrigger(final CaseEvent eventTrigger) {
        final CaseEventTrigger caseTrigger = new CaseEventTrigger();

        caseTrigger.setId(eventTrigger.getId());
        caseTrigger.setName(eventTrigger.getName());
        caseTrigger.setDescription(eventTrigger.getDescription());
        caseTrigger.setShowSummary(eventTrigger.getShowSummary());
        caseTrigger.setShowEventNotes(eventTrigger.getShowEventNotes());
        caseTrigger.setEndButtonLabel(eventTrigger.getEndButtonLabel());
        caseTrigger.setCanSaveDraft(eventTrigger.getCanSaveDraft());
        return caseTrigger;
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);

        if (null == caseType) {
            throw new ResourceNotFoundException("Case type not found");
        }
        return caseType;
    }

    private CaseEvent getEventTrigger(String eventTriggerId, CaseType caseType) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, eventTriggerId);

        if (null == eventTrigger) {
            throw new ResourceNotFoundException(eventTriggerId + " is not a known event ID for the specified case type: " + caseType.getId());
        }
        return eventTrigger;
    }

    private CaseEventTrigger merge(StartEventTrigger startEventTrigger, String caseTypeId, String eventTriggerId, String caseReference) {
        if (startEventTrigger.getCaseDetails() == null) {
            throw new ResourceNotFoundException("Case not found");
        }

        final CaseType caseType = getCaseType(caseTypeId);
        final CaseEvent eventTrigger = getEventTrigger(eventTriggerId, caseType);
        final CaseEventTrigger caseEventTrigger = buildCaseEventTrigger(eventTrigger);
        caseEventTrigger.setCaseId(caseReference);
        caseEventTrigger.setCaseFields(mergeEventFields(startEventTrigger.getCaseDetails(), caseType, eventTrigger));
        caseEventTrigger.setEventToken(startEventTrigger.getToken());
        final List<WizardPage> wizardPageCollection = uiDefinitionRepository.getWizardPageCollection(caseTypeId,
                                                                                                     eventTriggerId);
        caseEventTrigger.setWizardPages(wizardPageCollection);
        return caseEventTrigger;
    }

    private List<CaseViewField> mergeEventFields(CaseDetails caseDetails, CaseType caseType, CaseEvent eventTrigger) {
        final List<CaseEventField> eventFields = eventTrigger.getCaseFields();
        final List<CaseField> caseFields = caseType.getCaseFields();

        return caseViewFieldBuilder.build(caseFields, eventFields, caseDetails != null ? caseDetails.getCaseDataAndMetadata() : null);
    }
}
