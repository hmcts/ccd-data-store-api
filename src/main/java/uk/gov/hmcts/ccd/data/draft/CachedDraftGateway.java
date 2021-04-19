package uk.gov.hmcts.ccd.data.draft;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;

@Service
@Qualifier(CachedDraftGateway.QUALIFIER)
public class CachedDraftGateway implements DraftGateway {

    public static final String QUALIFIER = "cached";

    @Autowired
    @SuppressWarnings("checkstyle:MemberName")
    private CachedDraftGateway _this;

    private final DraftGateway draftGateway;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Autowired
    public CachedDraftGateway(@Qualifier(DefaultDraftGateway.QUALIFIER)
                              DraftGateway draftGateway,
                              DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder) {
        this.draftGateway = draftGateway;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public Long create(CreateCaseDraftRequest draft) {
        return draftGateway.create(draft);
    }

    @Override
    @Cacheable("draftResponseCache")
    public DraftResponse get(String draftId) {
        return draftGateway.get(draftId);
    }

    @Override
    @Cacheable("draftResponseCaseDetailsCache")
    public CaseDetails getCaseDetails(String draftId) {
        DraftResponse draftResponse = _this.get(draftId);
        return draftResponseToCaseDetailsBuilder.build(draftResponse);
    }

    @Override
    public List<DraftResponse> getAll() {
        return draftGateway.getAll();
    }

    @Override
    @CacheEvict(value = {"draftResponseCache", "draftResponseCaseDetailsCache"}, key = "#draftId", allEntries = true)
    public DraftResponse update(UpdateCaseDraftRequest draft, String draftId) {
        return draftGateway.update(draft, draftId);
    }

    @Override
    @CacheEvict(value = {"draftResponseCache", "draftResponseCaseDetailsCache"}, allEntries = true)
    public void delete(String draftId) {
        draftGateway.delete(draftId);
    }
}
