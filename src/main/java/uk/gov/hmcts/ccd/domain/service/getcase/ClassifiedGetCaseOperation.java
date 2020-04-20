package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Optional;

@Service
@Qualifier("classified")
public class ClassifiedGetCaseOperation implements GetCaseOperation {


    private final GetCaseOperation getCaseOperation;
    private final SecurityClassificationService classificationService;

    public ClassifiedGetCaseOperation(@Qualifier("default") GetCaseOperation getCaseOperation,
                                      SecurityClassificationService classificationService) {
        this.getCaseOperation = getCaseOperation;
        this.classificationService = classificationService;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
                               .flatMap(classificationService::applyClassification);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return getCaseOperation.execute(caseReference)
                               .flatMap(classificationService::applyClassification);
    }
}
