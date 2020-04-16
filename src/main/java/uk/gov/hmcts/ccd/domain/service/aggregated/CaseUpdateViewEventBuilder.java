package uk.gov.hmcts.ccd.domain.service.aggregated;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewFieldBuilder;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Named
@Singleton
public class CaseUpdateViewEventBuilder {

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseViewFieldBuilder caseViewFieldBuilder;

    public CaseUpdateViewEventBuilder(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                      final UIDefinitionRepository uiDefinitionRepository,
                                      final EventTriggerService eventTriggerService,
                                      final CaseViewFieldBuilder caseViewFieldBuilder) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseViewFieldBuilder = caseViewFieldBuilder;
    }

    public CaseUpdateViewEvent build(StartEventTrigger startEventTrigger, String caseTypeId, String eventTriggerId, String caseReference) {
        if (startEventTrigger.getCaseDetails() == null) {
            throw new ResourceNotFoundException("Case not found");
        }

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);
        final CaseEvent eventTrigger = getEventTrigger(eventTriggerId, caseTypeDefinition);
        final CaseUpdateViewEvent caseUpdateViewEvent = buildCaseEventTrigger(eventTrigger);
        caseUpdateViewEvent.setCaseId(caseReference);
        caseUpdateViewEvent.setCaseFields(mergeEventFields(startEventTrigger.getCaseDetails(), caseTypeDefinition, eventTrigger));
        caseUpdateViewEvent.setEventToken(startEventTrigger.getToken());
        final List<WizardPage> wizardPageCollection = uiDefinitionRepository.getWizardPageCollection(caseTypeId,
                                                                                                     eventTriggerId);
        caseUpdateViewEvent.setWizardPages(wizardPageCollection);
        return caseUpdateViewEvent;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);

        if (null == caseTypeDefinition) {
            throw new ResourceNotFoundException("Case type not found");
        }
        return caseTypeDefinition;
    }

    private CaseEvent getEventTrigger(String eventTriggerId, CaseTypeDefinition caseTypeDefinition) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseTypeDefinition, eventTriggerId);

        if (null == eventTrigger) {
            throw new ResourceNotFoundException(eventTriggerId + " is not a known event ID for the specified case type: " + caseTypeDefinition.getId());
        }
        return eventTrigger;
    }

    private CaseUpdateViewEvent buildCaseEventTrigger(final CaseEvent eventTrigger) {
        final CaseUpdateViewEvent caseTrigger = new CaseUpdateViewEvent();

        caseTrigger.setId(eventTrigger.getId());
        caseTrigger.setName(eventTrigger.getName());
        caseTrigger.setDescription(eventTrigger.getDescription());
        caseTrigger.setShowSummary(eventTrigger.getShowSummary());
        caseTrigger.setShowEventNotes(eventTrigger.getShowEventNotes());
        caseTrigger.setEndButtonLabel(eventTrigger.getEndButtonLabel());
        caseTrigger.setCanSaveDraft(eventTrigger.getCanSaveDraft());
        return caseTrigger;
    }

    private List<CaseViewField> mergeEventFields(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition, CaseEvent eventTrigger) {
        final List<CaseEventFieldDefinition> eventFields = eventTrigger.getCaseFields();
        final List<CaseFieldDefinition> caseFieldDefinitions = caseTypeDefinition.getCaseFieldDefinitions();

        return caseViewFieldBuilder.build(caseFieldDefinitions, eventFields, caseDetails != null ? caseDetails.getCaseDataAndMetadata() : null);
    }

}
