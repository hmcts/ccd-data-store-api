package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.definition.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public CaseView execute(String jurisdictionId, String caseTypeId, String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference " + caseReference + " is not valid");
        }

        final CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        final CaseDetails caseDetails =
            getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                String.format(RESOURCE_NOT_FOUND, jurisdictionId, caseTypeId, caseReference)));

        final List<AuditEvent> events = listEventsOperation.execute(caseDetails);
        final CaseTabCollection caseTabCollection = uiDefinitionRepository.getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, events, caseType, caseTabCollection);
    }

    private CaseType getCaseType(String jurisdictionId, String caseTypeId) {
        return caseTypeService.getCaseTypeForJurisdiction(caseTypeId, jurisdictionId);
    }

    private CaseView merge(CaseDetails caseDetails, List<AuditEvent> events, CaseType caseType, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[caseTabCollection.getChannels().size()]));

        CaseState caseState = caseTypeService.findState(caseType, caseDetails.getState());
        caseView.setState(new ProfileCaseState(caseState.getId(), caseState.getName(), caseState.getDescription()));

        Jurisdiction jurisdiction = caseType.getJurisdiction();
        CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction(jurisdiction.getId(),
                                                                             jurisdiction.getName(),
                                                                             jurisdiction.getDescription());
        caseView.setCaseType(new CaseViewType(caseType.getId(),
                                              caseType.getName(),
                                              caseType.getDescription(),
                                              caseViewJurisdiction));

        List<CaseViewTab> tabs = caseTabCollection.getTabs().stream().map(tab -> {
            Stream<CaseTypeTabField> tabsWithRelevantFields = tab.getTabFields().stream()
                .filter(caseDetails::existsInData);
            List<CaseViewField> fields = tabsWithRelevantFields
                .map(buildCaseViewField(caseDetails)).collect(Collectors.toList());
            return new CaseViewTab(tab.getId(), tab.getLabel(), tab.getDisplayOrder(),
                                   fields.toArray(new CaseViewField[fields.size()]));
        }).collect(Collectors.toList());
        caseView.setTabs(tabs.toArray(new CaseViewTab[tabs.size()]));

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
                               .map(event -> {
                                   final CaseViewEvent caseEvent = new CaseViewEvent();
                                   caseEvent.setId(event.getId());
                                   caseEvent.setEventId(event.getEventId());
                                   caseEvent.setEventName(event.getEventName());
                                   caseEvent.setUserId(event.getUserId());
                                   caseEvent.setUserLastName(event.getUserLastName());
                                   caseEvent.setUserFirstName(event.getUserFirstName());
                                   caseEvent.setSummary(event.getSummary());
                                   caseEvent.setComment(event.getDescription());
                                   caseEvent.setTimestamp(event.getCreatedDate());
                                   caseEvent.setStateId(event.getStateId());
                                   caseEvent.setStateName(event.getStateName());
                                   return caseEvent;
                               })
                               .toArray(CaseViewEvent[]::new));

        return caseView;
    }

    private Function<CaseTypeTabField, CaseViewField> buildCaseViewField(CaseDetails caseDetails) {
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
            caseViewField.setValue(caseDetails.getData().get(caseField.getId()));
            return caseViewField;
        };
    }

}
