package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
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
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@Qualifier(DefaultGetDraftViewOperation.QUALIFIER)
public class DefaultGetDraftViewOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "defaultDraft";
    private static final String RESOURCE_NOT_FOUND //
        = "No draft found ( jurisdiction = '%s', caseType = '%s', draftId = '%s' )";

    private final GetDraftOperation getDraftOperation;

    @Autowired
    public DefaultGetDraftViewOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                                        final UIDefinitionRepository uiDefinitionRepository,
                                        final CaseTypeService caseTypeService,
                                        final UIDService uidService,
                                        @Qualifier(DefaultGetDraftOperation.QUALIFIER) final GetDraftOperation getDraftOperation) {
        super(getCaseOperation, uiDefinitionRepository, caseTypeService, uidService);
        this.getDraftOperation = getDraftOperation;
    }

    @Override
    public CaseView execute(String jurisdictionId, String caseTypeId, String draftId) {

        final CaseType caseType = getCaseType(jurisdictionId, caseTypeId);
        final CaseDetails caseDetails = getDraftOperation.execute(draftId).orElseThrow(
            () -> new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND,
                                                              jurisdictionId,
                                                              caseTypeId,
                                                              draftId)));

        final CaseTabCollection caseTabCollection = getCaseTabCollection(caseDetails.getCaseTypeId());

        return merge(caseDetails, caseType, caseTabCollection);
    }

    private CaseView merge(CaseDetails caseDetails, CaseType caseType, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getReference().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[0]));

        caseView.setCaseType(CaseViewType.createFrom(caseType));
        caseView.setTabs(getTabs(caseDetails, caseDetails.getData(), caseTabCollection));

        final CaseViewTrigger[] triggers = caseType.getEvents()
            .stream()
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

        return caseView;
    }

}
