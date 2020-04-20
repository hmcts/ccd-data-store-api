package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import java.util.ArrayList;

import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEventBuilder.anCaseViewTrigger;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.CASE_HISTORY_VIEWER;

@Service
@Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER)
public class DefaultGetCaseViewFromDraftOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "defaultDraft";
    protected static final String DELETE = "DELETE";
    private static final CaseViewActionableEvent DELETE_TRIGGER = anCaseViewTrigger()
        .withId(DELETE)
        .withName("Delete")
        .withDescription("Delete draft")
        .withOrder(2)
        .build();
    private static final String RESUME = "Resume";

    private final DraftGateway draftGateway;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Autowired
    public DefaultGetCaseViewFromDraftOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                                                final UIDefinitionRepository uiDefinitionRepository,
                                                final CaseTypeService caseTypeService,
                                                final UIDService uidService,
                                                @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                                final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder,
                                                final ObjectMapperService objectMapperService,
                                                final CompoundFieldOrderService compoundFieldOrderService) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService, objectMapperService, compoundFieldOrderService);
        this.draftGateway = draftGateway;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public CaseView execute(String draftId) {
        final DraftResponse draftResponse = draftGateway.get(draftId);

        final CaseDetails caseDetails = draftResponseToCaseDetailsBuilder.build(draftResponse);

        CaseTypeDefinition caseTypeDefinition = getCaseType(draftResponse.getCaseTypeId());

        final CaseViewActionableEvent resumeTrigger = buildResumeTriggerFromDraft(draftResponse);

        final CaseTypeTabsDefinition caseTypeTabsDefinition = getCaseTabCollection(draftResponse.getCaseTypeId());

        CaseViewEvent[] events = buildEventsFromDraft(draftResponse);

        return merge(caseDetails, resumeTrigger, events, caseTypeDefinition, caseTypeTabsDefinition);
    }

    private CaseViewEvent[] buildEventsFromDraft(DraftResponse draftResponse) {
        ArrayList<CaseViewEvent> events = new ArrayList<>();
        if (draftResponse.getUpdated() != null) {
            CaseViewEvent lastUpdatedEvent = new CaseViewEvent();
            lastUpdatedEvent.setEventId("Draft updated");
            lastUpdatedEvent.setEventName("Draft updated");
            lastUpdatedEvent.setStateId("Draft");
            lastUpdatedEvent.setStateName("Draft");
            lastUpdatedEvent.setTimestamp(draftResponse.getUpdated());
            lastUpdatedEvent.setUserId("");
            events.add(lastUpdatedEvent);
        }
        if (draftResponse.getCreated() != null) {
            CaseViewEvent createEvent = new CaseViewEvent();
            createEvent.setEventId("Draft created");
            createEvent.setEventName("Draft created");
            createEvent.setStateId("Draft");
            createEvent.setStateName("Draft");
            createEvent.setUserId("");
            createEvent.setTimestamp(draftResponse.getCreated());
            events.add(createEvent);
        }
        return events.toArray(new CaseViewEvent[events.size()]);
    }

    private CaseViewActionableEvent buildResumeTriggerFromDraft(DraftResponse draftResponse) {
        return anCaseViewTrigger()
            .withId(draftResponse.getDocument().getEventTriggerId())
            .withName(RESUME)
            .withDescription(draftResponse.getDocument().getCaseDataContent().getEvent().getDescription())
            .withOrder(1)
            .build();
    }

    private CaseView merge(CaseDetails caseDetails, CaseViewActionableEvent resumeTrigger, CaseViewEvent[] events, CaseTypeDefinition caseTypeDefinition,
                           CaseTypeTabsDefinition caseTypeTabsDefinition) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getId().toString());
        caseView.setChannels(caseTypeTabsDefinition.getChannels().toArray(new String[0]));

        caseView.setCaseType(CaseViewType.createFrom(caseTypeDefinition));
        if (caseTypeTabsDefinition.hasTabFieldType(CASE_HISTORY_VIEWER)) {
            hydrateHistoryField(caseDetails, caseTypeDefinition, Lists.newArrayList(events));
        }
        caseView.setTabs(getTabs(caseDetails, caseDetails.getData(), caseTypeTabsDefinition));
        caseView.setMetadataFields(getMetadataFields(caseTypeDefinition, caseDetails));

        caseView.setTriggers(new CaseViewActionableEvent[]{resumeTrigger, DELETE_TRIGGER});
        caseView.setEvents(events);

        return caseView;
    }

}
