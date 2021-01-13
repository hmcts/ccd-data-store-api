package uk.gov.hmcts.ccd.domain.service.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;

import java.util.Map;

@Service
public class PaginatedSearchMetaDataOperation {
    private final CaseDetailsRepository caseDetailsRepository;

    @Autowired
    public PaginatedSearchMetaDataOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                                final CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    public PaginatedSearchMetadata execute(MetaData metaData, Map<String, String> criteria) {
        return caseDetailsRepository.getPaginatedSearchMetadata(metaData, criteria);
    }

}
