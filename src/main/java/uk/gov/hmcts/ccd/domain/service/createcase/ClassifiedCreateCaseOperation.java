package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Map;

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
    public CaseDetails createCaseDetails(String uid,
                                         String jurisdictionId,
                                         String caseTypeId,
                                         Event event,
                                         Map<String, JsonNode> data,
                                         Boolean ignoreWarning,
                                         String token) {
        final CaseDetails caseDetails = createCaseOperation.createCaseDetails(uid,
                                                                              jurisdictionId,
                                                                              caseTypeId,
                                                                              event,
                                                                              data,
                                                                              ignoreWarning,
                                                                              token);
        if (null == caseDetails) {
            return null;
        }
        return classificationService.applyClassification(caseDetails).orElse(null);
    }
}
