package uk.gov.hmcts.ccd.domain.service.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;

@Service
@Qualifier("default")
public class DefaultSearchOperation implements SearchOperation {
    private final CaseDetailsRepository caseDetailsRepository;

    @Autowired
    public DefaultSearchOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                          CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria) {
        return caseDetailsRepository.findByMetaDataAndFieldData(metaData, criteria);
    }
}
