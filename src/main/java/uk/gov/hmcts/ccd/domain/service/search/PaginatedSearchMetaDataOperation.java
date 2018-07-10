package uk.gov.hmcts.ccd.domain.service.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;

import java.util.Map;

@Service
public class PaginatedSearchMetaDataOperation {
    private final GetDraftsOperation getDraftsOperation;
    private final CaseDetailsRepository caseDetailsRepository;
    private final ApplicationParams applicationParams;

    @Autowired
    public PaginatedSearchMetaDataOperation(@Qualifier(DefaultGetDraftsOperation.QUALIFIER) GetDraftsOperation getDraftsOperation,
                                            @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                            final ApplicationParams applicationParams) {
        this.getDraftsOperation = getDraftsOperation;
        this.caseDetailsRepository = caseDetailsRepository;
        this.applicationParams = applicationParams;
    }

    public PaginatedSearchMetadata execute(MetaData metaData, Map<String, String> criteria) {
        PaginatedSearchMetadata paginatedSearchMetadata = caseDetailsRepository.getPaginatedSearchMetadata(metaData, criteria);
        calculatePaginationWithDrafts(metaData, paginatedSearchMetadata);
        return paginatedSearchMetadata;
    }

    private void calculatePaginationWithDrafts(MetaData metaData, PaginatedSearchMetadata paginatedSearchMetadata) {
        Integer totalResultsCount = paginatedSearchMetadata.getTotalResultsCount();
        totalResultsCount += (int) getDraftsOperation.execute()
            .stream()
            .filter(d -> caseTypeIdsEqual(metaData, d))
            .count();
        paginatedSearchMetadata.setTotalResultsCount(totalResultsCount);
        paginatedSearchMetadata.setTotalPagesCount((int) Math.ceil((double) totalResultsCount / applicationParams.getPaginationPageSize()));
    }

    private boolean caseTypeIdsEqual(MetaData metaData, DraftResponse d) {
        return d.getDocument().getCaseTypeId().equals(metaData.getCaseTypeId());
    }

}
