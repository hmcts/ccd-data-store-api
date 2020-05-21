package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.CASE_HISTORY_VIEWER;

@Service
@Qualifier(DefaultGetCaseViewOperation.QUALIFIER)
public class DefaultGetCaseViewOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "defaultCase";

    private final GetEventsOperation getEventsOperation;
    private final CaseTypeService caseTypeService;
    private final EventTriggerService eventTriggerService;

    @Autowired
    public DefaultGetCaseViewOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
                                       @Qualifier("authorised") GetEventsOperation getEventsOperation,
                                       UIDefinitionRepository uiDefinitionRepository,
                                       CaseTypeService caseTypeService,
                                       EventTriggerService eventTriggerService,
                                       UIDService uidService,
                                       ObjectMapperService objectMapperService,
                                       CompoundFieldOrderService compoundFieldOrderService,
                                       FieldProcessorService fieldProcessorService) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService, objectMapperService, compoundFieldOrderService, fieldProcessorService);
        this.getEventsOperation = getEventsOperation;
        this.caseTypeService = caseTypeService;
        this.eventTriggerService = eventTriggerService;
    }

    @Override
    public CaseView execute(String caseReference) {
        validateCaseReference(caseReference);

        final CaseDetails caseDetails = getCaseDetails(caseReference);

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getJurisdiction(), caseDetails.getCaseTypeId());
        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);
        final CaseTypeTabsDefinition caseTypeTabsDefinition = getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, events, caseTypeDefinition, caseTypeTabsDefinition);
    }

    private CaseView merge(CaseDetails caseDetails, List<AuditEvent> events,
                           CaseTypeDefinition caseTypeDefinition,
                           CaseTypeTabsDefinition caseTypeTabsDefinition) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTypeTabsDefinition.getChannels().toArray(new String[0]));

        CaseStateDefinition caseStateDefinition = caseTypeService.findState(caseTypeDefinition, caseDetails.getState());
        caseView.setState(new ProfileCaseState(caseStateDefinition.getId(),
            caseStateDefinition.getName(), caseStateDefinition.getDescription(),
            caseStateDefinition.getTitleDisplay()));

        caseView.setCaseType(CaseViewType.createFrom(caseTypeDefinition));
        final CaseViewEvent[] caseViewEvents = convertToCaseViewEvent(events);
        if (caseTypeTabsDefinition.hasTabFieldType(CASE_HISTORY_VIEWER)) {
            hydrateHistoryField(caseDetails, caseTypeDefinition, Lists.newArrayList(caseViewEvents));
        }
        caseView.setTabs(getTabs(caseDetails, caseDetails.getCaseDataAndMetadata(), caseTypeTabsDefinition));
        caseView.setMetadataFields(getMetadataFields(caseTypeDefinition, caseDetails));

        final CaseViewActionableEvent[] actionableEvents = caseTypeDefinition.getEvents()
            .stream()
            .filter(event -> eventTriggerService.isPreStateValid(caseStateDefinition.getId(), event))
            .map(event -> {
                final CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
                caseViewActionableEvent.setId(event.getId());
                caseViewActionableEvent.setName(event.getName());
                caseViewActionableEvent.setDescription(event.getDescription());
                caseViewActionableEvent.setOrder(event.getDisplayOrder());
                return caseViewActionableEvent;
            })
            .toArray(CaseViewActionableEvent[]::new);
        caseView.setActionableEvents(actionableEvents);

        caseView.setEvents(caseViewEvents);

        return caseView;
    }

    private CaseViewEvent[] convertToCaseViewEvent(List<AuditEvent> events) {
        return events
            .stream()
            .map(CaseViewEvent::createFrom)
            .toArray(size -> new CaseViewEvent[size]);
    }

}
