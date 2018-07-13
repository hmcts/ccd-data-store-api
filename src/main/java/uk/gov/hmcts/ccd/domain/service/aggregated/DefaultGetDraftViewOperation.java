package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTriggerBuilder.aCaseViewTrigger;
import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetailsBuilder.aCaseDetails;

@Service
@Qualifier(DefaultGetDraftViewOperation.QUALIFIER)
public class DefaultGetDraftViewOperation extends AbstractDefaultGetCaseViewOperation implements GetCaseViewOperation {

    public static final String QUALIFIER = "defaultDraft";
    private static final String RESOURCE_NOT_FOUND //
        = "No draft found ( jurisdiction = '%s', caseType = '%s', draftId = '%s' )";
    private static final CaseViewTrigger DELETE_TRIGGER = aCaseViewTrigger()
        .withName("Delete")
        .withDescription("Delete draft")
        .withOrder(2)
        .build();
    private static final String RESUME = "Resume";

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
        final DraftResponse draftResponse = getDraftOperation.execute(draftId).orElseThrow(
            () -> new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND,
                                                              jurisdictionId,
                                                              caseTypeId,
                                                              draftId)));

        final CaseDetails caseDetails = buildCaseDetailsFromDraft(draftResponse);
        final CaseViewTrigger resumeTrigger = buildResumeTriggerFromDraft(draftResponse);

        final CaseTabCollection caseTabCollection = getCaseTabCollection(getCaseTypeIdFromDraft(draftResponse));

        return merge(caseDetails, resumeTrigger, caseType, caseTabCollection);
    }

    private CaseViewTrigger buildResumeTriggerFromDraft(DraftResponse draftResponse) {
        return aCaseViewTrigger()
            .withId(draftResponse.getDocument().getEventTriggerId())
            .withName(RESUME)
            .withDescription(draftResponse.getDocument().getCaseDataContent().getEvent().getDescription())
            .withOrder(1)
            .build();
    }

    private String getCaseTypeIdFromDraft(DraftResponse draftResponse) {
        return draftResponse.getDocument().getCaseTypeId();
    }

    private CaseView merge(CaseDetails caseDetails, CaseViewTrigger resumeTrigger, CaseType caseType, CaseTabCollection caseTabCollection) {
        CaseView caseView = new CaseView();
        caseView.setCaseId(caseDetails.getId().toString());
        caseView.setChannels(caseTabCollection.getChannels().toArray(new String[0]));

        caseView.setCaseType(CaseViewType.createFrom(caseType));
        caseView.setTabs(getTabs(caseDetails, caseDetails.getData(), caseTabCollection));

        caseView.setTriggers(new CaseViewTrigger[] {resumeTrigger, DELETE_TRIGGER});

        return caseView;
    }

    private CaseDetails buildCaseDetailsFromDraft(DraftResponse draftResponse) {
        CaseDraft document = draftResponse.getDocument();
        return aCaseDetails()
            .withId(draftResponse.getId())
            .withCaseTypeId(document.getCaseTypeId())
            .withJurisdiction(document.getJurisdictionId())
            .withData(document.getCaseDataContent().getData())
            .build();
    }

}
