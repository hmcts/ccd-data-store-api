package uk.gov.hmcts.ccd.domain.service.createevent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Optional;

@Service
@Qualifier("classified")
public class ClassifiedCreateEventOperation implements CreateEventOperation {
    private final CreateEventOperation createEventOperation;
    private final SecurityClassificationService classificationService;

    @Autowired
    public ClassifiedCreateEventOperation(@Qualifier("default") CreateEventOperation createEventOperation,
                                          SecurityClassificationService classificationService) {

        this.createEventOperation = createEventOperation;
        this.classificationService = classificationService;
    }

    @Override
    public CaseDetails createCaseEvent(String uid,
                                       String jurisdictionId,
                                       String caseTypeId,
                                       String caseReference,
                                       CaseDataContent content) {
        final CaseDetails caseDetails = createEventOperation.createCaseEvent(uid,
                                                                           jurisdictionId,
                                                                           caseTypeId,
                                                                           caseReference,
                                                                           content);
        return Optional.ofNullable(caseDetails)
                       .flatMap(classificationService::applyClassification)
                       .orElse(null);
    }
}
