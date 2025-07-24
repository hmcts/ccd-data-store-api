package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

@Service
@Qualifier("classified")
public class ClassifiedGetCaseOperation implements GetCaseOperation {


    private final GetCaseOperation getCaseOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final ApplicationParams applicationParams;

    public ClassifiedGetCaseOperation(@Qualifier("default") GetCaseOperation getCaseOperation,
                                      SecurityClassificationServiceImpl classificationService,
                                      ApplicationParams applicationParams) {
        this.getCaseOperation = getCaseOperation;
        this.classificationService = classificationService;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        Optional<CaseDetails> caseDetails = getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference);
        return applicationParams.isPocFeatureEnabled()
                ? caseDetails
                : caseDetails.flatMap(classificationService::applyClassification);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return applicationParams.isPocFeatureEnabled()
                ? getCaseOperation.execute(caseReference)
                : getCaseOperation.execute(caseReference)
                .flatMap(classificationService::applyClassification);
    }
}
