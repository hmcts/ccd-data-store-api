package uk.gov.hmcts.ccd.domain.service.search;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;

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
        calculatePaginationWithDrafts(paginatedSearchMetadata);
        return paginatedSearchMetadata;
    }

    private void calculatePaginationWithDrafts(PaginatedSearchMetadata paginatedSearchMetadata) {
        Integer totalResultsCount = paginatedSearchMetadata.getTotalResultsCount();
        totalResultsCount += getDraftsOperation.execute().size();
        paginatedSearchMetadata.setTotalResultsCount(totalResultsCount);
        paginatedSearchMetadata.setTotalPagesCount((int) Math.ceil((double) totalResultsCount / applicationParams.getPaginationPageSize()));
    }
}
