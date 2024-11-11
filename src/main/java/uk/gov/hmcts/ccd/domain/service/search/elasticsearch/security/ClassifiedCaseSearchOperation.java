package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchCaseSearchOperation;

@Service
@Qualifier("classified")
public class ClassifiedCaseSearchOperation implements CaseSearchOperation {
    private final CaseSearchOperation caseSearchOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final ApplicationParams applicationParams;

    @Autowired
    public ClassifiedCaseSearchOperation(@Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER)
                                                 CaseSearchOperation caseSearchOperation,
                                         SecurityClassificationServiceImpl classificationService,
                                         ApplicationParams applicationParams) {

        this.caseSearchOperation = caseSearchOperation;
        this.classificationService = classificationService;
        this.applicationParams = applicationParams;
    }

    @Override
    public CaseSearchResult execute(CrossCaseTypeSearchRequest request, boolean dataClassification) {
        final CaseSearchResult results = caseSearchOperation.execute(request, dataClassification);

        if (results == null) {
            return new CaseSearchResult();
        }

        List<CaseDetails> classifiedCases = applicationParams.isPocFeatureEnabled()
                ? results.getCases()
                : results.getCases()
                .stream()
                .map(classificationService::applyClassification)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return new CaseSearchResult(results.getTotal(),
            classifiedCases, results.getCaseTypesResults());
    }
}
