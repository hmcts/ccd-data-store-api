package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.search.CreatorSearchOperation.QUALIFIER;

@Service
@Qualifier(QUALIFIER)
public class CreatorSearchOperation implements SearchOperation {

    public static final String QUALIFIER = "creator";
    private final SearchOperation searchOperation;
    private final CaseAccessService caseAccessService;

    @Autowired
    public CreatorSearchOperation(@Qualifier("authorised") SearchOperation searchOperation,
                                  CaseAccessService caseAccessService) {

        this.searchOperation = searchOperation;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria) {
        final List<CaseDetails> results = searchOperation.execute(metaData, criteria);

        if (null == results) {
            return Lists.newArrayList();
        }

        return results.stream()
            .filter(caseAccessService::canUserAccess)
            .collect(Collectors.toList());
    }
}
