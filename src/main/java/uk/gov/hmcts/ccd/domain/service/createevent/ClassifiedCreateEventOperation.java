package uk.gov.hmcts.ccd.domain.service.createevent;

import java.util.Optional;

import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;

@Service
@Qualifier("classified")
public class ClassifiedCreateEventOperation implements CreateEventOperation {
    private final CreateEventOperation createEventOperation;
    private final SecurityClassificationServiceImpl classificationService;

    @Autowired
    public ClassifiedCreateEventOperation(@Qualifier("default") CreateEventOperation createEventOperation,
                                          SecurityClassificationServiceImpl classificationService) {

        this.createEventOperation = createEventOperation;
        this.classificationService = classificationService;
    }

    @Transactional
    @Override
    public CaseDetails createCaseEvent(String caseReference,
                                       CaseDataContent content) {
        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
                                                                           content);
        return Optional.ofNullable(caseDetails)
                       .flatMap(classificationService::applyClassification)
                        .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }
}
