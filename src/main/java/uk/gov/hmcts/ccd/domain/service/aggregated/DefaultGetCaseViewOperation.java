package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;

import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.CASE_HISTORY_VIEWER;

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
                                       CompoundFieldOrderService compoundFieldOrderService) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService, objectMapperService, compoundFieldOrderService);
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
        final CaseTabCollection caseTabCollection = getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, events, caseTypeDefinition, caseTabCollection);
    }

    private CaseView merge(CaseDetails caseDetails, List<AuditEvent> events, CaseTypeDefinition caseTypeDefinition, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[0]));

        CaseStateDefinition caseStateDefinition = caseTypeService.findState(caseTypeDefinition, caseDetails.getState());
        caseView.setState(new ProfileCaseState(caseStateDefinition.getId(), caseStateDefinition.getName(), caseStateDefinition.getDescription(), caseStateDefinition.getTitleDisplay()));

        caseView.setCaseType(CaseViewType.createFrom(caseTypeDefinition));
        final CaseViewEvent[] caseViewEvents = convertToCaseViewEvent(events);
        if (caseTabCollection.hasTabFieldType(CASE_HISTORY_VIEWER)) {
            hydrateHistoryField(caseDetails, caseTypeDefinition, Lists.newArrayList(caseViewEvents));
        }
        caseView.setTabs(getTabs(caseDetails, caseDetails.getCaseDataAndMetadata(), caseTabCollection));
        caseView.setMetadataFields(getMetadataFields(caseTypeDefinition, caseDetails));

        final CaseViewTrigger[] triggers = caseTypeDefinition.getEvents()
            .stream()
            .filter(event -> eventTriggerService.isPreStateValid(caseStateDefinition.getId(), event))
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
