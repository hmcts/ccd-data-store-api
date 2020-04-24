package uk.gov.hmcts.ccd.domain.model.aggregated;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
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

    public CaseUpdateViewEvent build(StartEventResult startEventResult, String caseTypeId, String eventId, String caseReference) {
        if (startEventResult.getCaseDetails() == null) {
            throw new ResourceNotFoundException("Case not found");
        }

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);
        final CaseEventDefinition caseEventDefinition = getCaseEventDefinition(eventId, caseTypeDefinition);
        final CaseUpdateViewEvent caseUpdateViewEvent = buildCaseUpdateEvent(caseEventDefinition);
        caseUpdateViewEvent.setCaseId(caseReference);
        caseUpdateViewEvent.setCaseFields(mergeEventFields(startEventResult.getCaseDetails(), caseTypeDefinition, caseEventDefinition));
        caseUpdateViewEvent.setEventToken(startEventResult.getToken());
        final List<WizardPage> wizardPageCollection = uiDefinitionRepository.getWizardPageCollection(caseTypeId,
                                                                                                     eventId);
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

    private CaseEventDefinition getCaseEventDefinition(String eventId, CaseTypeDefinition caseTypeDefinition) {
        final CaseEventDefinition caseEventDefinition = eventTriggerService.findCaseEvent(caseTypeDefinition, eventId);

        if (null == caseEventDefinition) {
            throw new ResourceNotFoundException(eventId + " is not a known event ID for the specified case type: " + caseTypeDefinition.getId());
        }
        return caseEventDefinition;
    }

    private CaseUpdateViewEvent buildCaseUpdateEvent(final CaseEventDefinition eventTrigger) {
        final CaseUpdateViewEvent caseUpdateViewEvent = new CaseUpdateViewEvent();

        caseUpdateViewEvent.setId(eventTrigger.getId());
        caseUpdateViewEvent.setName(eventTrigger.getName());
        caseUpdateViewEvent.setDescription(eventTrigger.getDescription());
        caseUpdateViewEvent.setShowSummary(eventTrigger.getShowSummary());
        caseUpdateViewEvent.setShowEventNotes(eventTrigger.getShowEventNotes());
        caseUpdateViewEvent.setEndButtonLabel(eventTrigger.getEndButtonLabel());
        caseUpdateViewEvent.setCanSaveDraft(eventTrigger.getCanSaveDraft());
        return caseUpdateViewEvent;
    }

    private List<CaseViewField> mergeEventFields(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition, CaseEventDefinition caseEventDefinition) {
        final List<CaseEventFieldDefinition> eventFields = caseEventDefinition.getCaseFields();
        final List<CaseFieldDefinition> caseFieldDefinitions = caseTypeDefinition.getCaseFieldDefinitions();

        return caseViewFieldBuilder.build(caseFieldDefinitions, eventFields, caseDetails != null ? caseDetails.getCaseDataAndMetadata() : null);
    }

}
