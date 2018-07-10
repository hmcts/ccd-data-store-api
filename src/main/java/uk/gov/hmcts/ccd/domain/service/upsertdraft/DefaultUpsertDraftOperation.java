package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import javax.inject.Inject;

import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftBuilder.aCreateCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftResponseBuilder.aDraftResponse;
import static uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftBuilder.anUpdateCaseDraft;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {

    private final DraftGateway draftGateway;
    private final ApplicationParams applicationParams;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                       ApplicationParams applicationParams) {
        this.draftGateway = draftGateway;
        this.applicationParams = applicationParams;
    }

    @Override
    public DraftResponse executeSave(final String uid,
                             final String jurisdictionId,
                             final String caseTypeId,
                             final String eventTriggerId,
                             final CaseDataContent caseDataContent) {
        return aDraftResponse()
            .withId(draftGateway.save(buildCreateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent)))
            .build();
    }

    @Override
    public DraftResponse executeUpdate(final String uid,
                               final String jurisdictionId,
                               final String caseTypeId,
                               final String eventTriggerId,
                               final String draftId,
                               final CaseDataContent caseDataContent) {
        return draftGateway.update(buildUpdateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent),
                                   draftId);
    }

    private CreateCaseDraft buildCreateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        return aCreateCaseDraft()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .withMaxStaleDays(applicationParams.getDraftMaxStaleDays())
            .build();
    }

    private UpdateCaseDraft buildUpdateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        return anUpdateCaseDraft()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .build();
    }

}
