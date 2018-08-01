package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;

import java.util.List;

@Service
@Qualifier(DefaultGetCaseViewOperation.QUALIFIER)
public class DefaultGetCaseViewOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "default";

    private final GetEventsOperation getEventsOperation;
    private final CaseTypeService caseTypeService;
    private final EventTriggerService eventTriggerService;

    @Autowired
    public DefaultGetCaseViewOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) GetCaseOperation getCaseOperation,
                                       @Qualifier("authorised") GetEventsOperation getEventsOperation,
                                       UIDefinitionRepository uiDefinitionRepository,
                                       CaseTypeService caseTypeService,
                                       EventTriggerService eventTriggerService,
                                       UIDService uidService) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService);
        this.getEventsOperation = getEventsOperation;
        this.caseTypeService = caseTypeService;
        this.eventTriggerService = eventTriggerService;
    }

    @Override
    public CaseView execute(String jurisdictionId, String caseTypeId, String caseReference) {
        validateCaseReference(caseReference);

        final CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        final CaseDetails caseDetails = getCaseDetails(jurisdictionId, caseTypeId, caseReference);

        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);
        final CaseTabCollection caseTabCollection = getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, events, caseType, caseTabCollection);
    }

    private CaseView merge(CaseDetails caseDetails, List<AuditEvent> events, CaseType caseType, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[0]));

        CaseState caseState = caseTypeService.findState(caseType, caseDetails.getState());
        caseView.setState(new ProfileCaseState(caseState.getId(), caseState.getName(), caseState.getDescription()));

        caseView.setCaseType(CaseViewType.createFrom(caseType));
        caseView.setTabs(getTabs(caseDetails, caseDetails.getCaseDataAndMetadata(), caseTabCollection));
        caseView.setMetadataFields(getMetadataFields(caseType, caseDetails));

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

}
