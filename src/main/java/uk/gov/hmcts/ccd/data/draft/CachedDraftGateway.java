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

import javax.inject.Singleton;

@Service
@Singleton
@Qualifier(CachedDraftGateway.QUALIFIER)
public class CachedDraftGateway implements DraftGateway {

    public static final String QUALIFIER = "cached";

    private final DraftGateway draftGateway;
    private final DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Autowired
    public CachedDraftGateway(@Qualifier(DefaultDraftGateway.QUALIFIER)
                              final DraftGateway draftGateway,
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
        DraftResponse response = draftGateway.update(draft, draftId);
        evictSingleCacheValue(draftId);
        return response;
    }

    @Override
    public void delete(String draftId) {
        draftGateway.delete(draftId);
        evictSingleCacheValue(draftId);
    }

    @CacheEvict(value = "draftResponseCache")
    public void evictSingleCacheValue(String draftId) {

    }
}
