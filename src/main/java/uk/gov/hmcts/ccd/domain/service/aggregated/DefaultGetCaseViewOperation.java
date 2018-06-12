package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.listevents.ListEventsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@Qualifier(DefaultGetCaseViewOperation.QUALIFIER)
public class DefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "default";
    private static final String RESOURCE_NOT_FOUND //
        = "No case found ( jurisdiction = '%s', case type = '%s', case reference = '%s' )";

    private final GetCaseOperation getCaseOperation;
    private final ListEventsOperation listEventsOperation;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseTypeService caseTypeService;
    private final EventTriggerService eventTriggerService;
    private final UIDService uidService;

    @Autowired
    public DefaultGetCaseViewOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
                                       @Qualifier("authorised") ListEventsOperation listEventsOperation,
                                       UIDefinitionRepository uiDefinitionRepository,
                                       CaseTypeService caseTypeService,
                                       EventTriggerService eventTriggerService,
                                       UIDService uidService) {
        this.getCaseOperation = getCaseOperation;
        this.listEventsOperation = listEventsOperation;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseTypeService = caseTypeService;
        this.eventTriggerService = eventTriggerService;
        this.uidService = uidService;
    }

    @Override
    public CaseView execute(String jurisdictionId, String caseTypeId, String caseReference) {
        validateCaseReference(caseReference);

        final CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        final CaseDetails caseDetails = getCaseDetails(jurisdictionId, caseTypeId, caseReference);

        final List<AuditEvent> events = listEventsOperation.execute(caseDetails);
        final CaseTabCollection caseTabCollection = getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, events, caseType, caseTabCollection);
    }

    @Override
    public CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId) {
        validateCaseReference(caseReference);

        CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        CaseDetails caseDetails = getCaseDetails(jurisdictionId, caseTypeId, caseReference);

        AuditEvent event = listEventsOperation.execute(jurisdictionId, caseTypeId, eventId);

        return merge(caseDetails, event, caseType);
    }

    private void validateCaseReference(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference " + caseReference + " is not valid");
        }
    }

    private CaseType getCaseType(String jurisdictionId, String caseTypeId) {
        return caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
    }

    private CaseDetails getCaseDetails(String jurisdictionId, String caseTypeId, String caseReference) {
        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(RESOURCE_NOT_FOUND, jurisdictionId, caseTypeId, caseReference)));
    }

    private CaseTabCollection getCaseTabCollection(String caseTypeId) {
        return uiDefinitionRepository.getCaseTabCollection(caseTypeId);
    }

    private CaseView merge(CaseDetails caseDetails, List<AuditEvent> events, CaseType caseType, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[0]));

        CaseState caseState = caseTypeService.findState(caseType, caseDetails.getState());
        caseView.setState(new ProfileCaseState(caseState.getId(), caseState.getName(), caseState.getDescription()));

        caseView.setCaseType(CaseViewType.createFrom(caseType));
        caseView.setTabs(getTabs(caseDetails, caseDetails.getData(), caseTabCollection));

        final CaseViewTrigger[] triggers = caseType.getEvents()
            .stream()
            .filter(event -> eventTriggerService.isPreStateValid(caseState.getId(), event))
            .map(event -> {
                final CaseViewTrigger trigger = new CaseViewTrigger();
                trigger.setId(event.getId());
                trigger.setName(event.getName());
                trigger.setDescription(event.getDescription());
                trigger.setOrder(event.getDisplayOrder());
                return trigger;
            })
            .toArray(CaseViewTrigger[]::new);
        caseView.setTriggers(triggers);

        caseView.setEvents(events
            .stream()
            .map(CaseViewEvent::createFrom)
            .toArray(CaseViewEvent[]::new));

        return caseView;
    }

    private Predicate<CaseTypeTabField> filterCaseTabFieldsBasedOnSecureData(CaseDetails caseDetails) {
        return caseDetails::existsInData;
    }

    private CaseHistoryView merge(CaseDetails caseDetails, AuditEvent event, CaseType caseType) {
        CaseHistoryView caseHistoryView = new CaseHistoryView();
        caseHistoryView.setCaseId(String.valueOf(caseDetails.getReference()));
        caseHistoryView.setCaseType(CaseViewType.createFrom(caseType));
        caseHistoryView.setTabs(getTabs(caseDetails, event.getData()));
        caseHistoryView.setEvent(CaseViewEvent.createFrom(event));

        return caseHistoryView;
    }

    private CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, JsonNode> data) {
        return getTabs(caseDetails, data, getCaseTabCollection(caseDetails.getCaseTypeId()));
    }

    private CaseViewTab[] getTabs(CaseDetails caseDetails, Map<String, JsonNode> data, CaseTabCollection caseTabCollection) {
        return caseTabCollection.getTabs().stream().map(tab -> {
            CaseViewField[] caseViewFields = tab.getTabFields()
                .stream()
                .filter(filterCaseTabFieldsBasedOnSecureData(caseDetails))
                .map(buildCaseViewField(data))
                .toArray(CaseViewField[]::new);

            return new CaseViewTab(tab.getId(),
                tab.getLabel(),
                tab.getDisplayOrder(),
                caseViewFields,
                tab.getShowCondition());

        }).toArray(CaseViewTab[]::new);
    }


    private Function<CaseTypeTabField, CaseViewField> buildCaseViewField(Map<String, JsonNode> data) {
        return field -> {
            CaseViewField caseViewField = new CaseViewField();
            CaseField caseField = field.getCaseField();
            caseViewField.setId(caseField.getId());
            caseViewField.setLabel(caseField.getLabel());
            caseViewField.setFieldType(caseField.getFieldType());
            caseViewField.setHidden(caseField.getHidden());
            caseViewField.setHintText(caseField.getHintText());
            caseViewField.setSecurityLabel(caseField.getSecurityLabel());
            caseViewField.setValidationExpression(caseField.getFieldType().getRegularExpression());
            caseViewField.setOrder(field.getDisplayOrder());
            caseViewField.setShowCondition(field.getShowCondition());
            caseViewField.setValue(data.get(caseField.getId()));
            return caseViewField;
        };
    }

}
