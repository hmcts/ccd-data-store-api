package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@Qualifier("default")
public class DefaultGetEventTriggerOperation implements GetEventTriggerOperation {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseViewFieldBuilder caseViewFieldBuilder;
    private final StartEventOperation startEventOperation;

    @Autowired
    public DefaultGetEventTriggerOperation(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                           final EventTriggerService eventTriggerService,
                                           final CaseViewFieldBuilder caseViewFieldBuilder,
                                           final UIDefinitionRepository uiDefinitionRepository,
                                           @Qualifier("authorised") final StartEventOperation startEventOperation) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseViewFieldBuilder = caseViewFieldBuilder;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.startEventOperation = startEventOperation;
    }

    @Override
    public CaseEventTrigger executeForCaseType(String uid,
                                               String jurisdictionId,
                                               String caseTypeId,
                                               String eventTriggerId,
                                               Boolean ignoreWarning) {
        return merge(startEventOperation.triggerStartForCaseType(uid,
                                                                 jurisdictionId,
                                                                 caseTypeId,
                                                                 eventTriggerId,
                                                                 ignoreWarning),
                                                                 caseTypeId,
                                                                 eventTriggerId,
                                                                 null);
    }

    @Override
    public CaseEventTrigger executeForCase(String uid,
                                           String jurisdictionId,
                                           String caseTypeId,
                                           String caseReference,
                                           String eventTriggerId,
                                           Boolean ignoreWarning) {
        return merge(startEventOperation.triggerStartForCase(uid,
                                                             jurisdictionId,
                                                             caseTypeId,
                                                             caseReference,
                                                             eventTriggerId,
                                                             ignoreWarning),
                                                             caseTypeId,
                                                             eventTriggerId,
                                                             caseReference);
    }

    @Override
    public CaseEventTrigger executeForDraft(String uid, String jurisdictionId, String caseTypeId, String draftReference, String eventTriggerId,
                                            Boolean ignoreWarning) {
        return merge(startEventOperation.triggerStartForDraft(uid,
                                                             jurisdictionId,
                                                             caseTypeId,
                                                             draftReference,
                                                             eventTriggerId,
                                                             ignoreWarning),
                                                             caseTypeId,
                                                             eventTriggerId,
                                                             draftReference);
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
