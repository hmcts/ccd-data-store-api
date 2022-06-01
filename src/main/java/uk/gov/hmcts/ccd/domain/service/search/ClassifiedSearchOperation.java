package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

@Service
@Qualifier(ClassifiedSearchOperation.QUALIFIER)
public class ClassifiedSearchOperation implements SearchOperation {
    public static final String QUALIFIER = "classified";

    private final SearchOperation searchOperation;
    private final SecurityClassificationServiceImpl classificationService;

    @Autowired
    public ClassifiedSearchOperation(@Qualifier(DefaultSearchOperation.QUALIFIER) SearchOperation searchOperation,
                                     SecurityClassificationServiceImpl classificationService) {

        this.searchOperation = searchOperation;
        this.classificationService = classificationService;
    }

    @Override
    public List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria) {
        final List<CaseDetails> results = searchOperation.execute(metaData, criteria);

        return streamResults(results);
    }

    @Override
    public List<CaseDetails> execute(MigrationParameters migrationParameters) {
        final List<CaseDetails> results = searchOperation.execute(migrationParameters);

        return streamResults(results);
    }

    private List<CaseDetails> streamResults(List<CaseDetails> results) {
        if (null == results) {
            return Lists.newArrayList();
        }

        return results.stream()
            .map(classificationService::applyClassification)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
}
