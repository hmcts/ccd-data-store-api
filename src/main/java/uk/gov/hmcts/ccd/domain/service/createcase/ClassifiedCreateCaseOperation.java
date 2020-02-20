package uk.gov.hmcts.ccd.domain.service.createcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

@Service
@Qualifier("classified")
public class ClassifiedCreateCaseOperation implements CreateCaseOperation {
    private final CreateCaseOperation createCaseOperation;
    private final SecurityClassificationService classificationService;

    @Autowired
    public ClassifiedCreateCaseOperation(@Qualifier("default") CreateCaseOperation createCaseOperation,
                                         SecurityClassificationService classificationService) {

        this.createCaseOperation = createCaseOperation;
        this.classificationService = classificationService;
    }

    @Override
    public CaseDetails createCaseDetails(String caseTypeId,
                                         CaseDataContent caseDataContent,
                                         Boolean ignoreWarning) {
        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId,
                                                                              caseDataContent,
                                                                              ignoreWarning);
        if (null == caseDetails) {
            return null;
        }
        return classificationService.applyClassification(caseDetails).orElse(null);
    }
}
