package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

@Service
@Qualifier("classified")
@Slf4j
public class ClassifiedSearchOperation implements SearchOperation {
    private final SearchOperation searchOperation;
    private final SecurityClassificationServiceImpl classificationService;

    @Autowired
    public ClassifiedSearchOperation(@Qualifier("default") SearchOperation searchOperation,
                                     SecurityClassificationServiceImpl classificationService) {

        this.searchOperation = searchOperation;
        this.classificationService = classificationService;
    }

    @Override
    public List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria) {
        final List<CaseDetails> results = searchOperation.execute(metaData, criteria);

        if (null == results) {
            return Lists.newArrayList();
        }
        log.info("SQL Search Classified result  {}", results);

        return results.stream()
                      .map(classificationService::applyClassification)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(Collectors.toList());
    }
}
