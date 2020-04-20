package uk.gov.hmcts.ccd.data.draft;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;

@Service
@Qualifier(CachedDraftGateway.QUALIFIER)
@RequestScope
public class CachedDraftGateway implements DraftGateway {

    public static final String QUALIFIER = "cached";

    private final DraftGateway draftGateway;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;
    private final Map<String, DraftResponse> drafts = newHashMap();

    @Autowired
    public CachedDraftGateway(@Qualifier(DefaultDraftGateway.QUALIFIER) DraftGateway defaultDraftGateway,
                              DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder) {
        this.draftGateway = defaultDraftGateway;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public Long create(CreateCaseDraftRequest draft) {
        return draftGateway.create(draft);
    }

    @Override
    public DraftResponse get(String draftId) {
        return drafts.computeIfAbsent(draftId, draftGateway::get);
    }

    @Override
    public CaseDetails getCaseDetails(String draftId) {
        DraftResponse draftResponse = get(draftId);
        return draftResponseToCaseDetailsBuilder.build(draftResponse);
    }

    @Override
    public List<DraftResponse> getAll() {
        return draftGateway.getAll();
    }

    @Override
    public DraftResponse update(UpdateCaseDraftRequest draft, String draftId) {
        return draftGateway.update(draft, draftId);
    }

    @Override
    public void delete(String draftId) {
        draftGateway.delete(draftId);
        drafts.remove(draftId);
    }
}
