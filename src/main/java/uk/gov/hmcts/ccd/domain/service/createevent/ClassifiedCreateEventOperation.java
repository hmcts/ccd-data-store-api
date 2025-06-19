package uk.gov.hmcts.ccd.domain.service.createevent;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;

@Service
@Qualifier("classified")
public class ClassifiedCreateEventOperation implements CreateEventOperation {
    private final CreateEventOperation createEventOperation;
    private final SecurityClassificationServiceImpl classificationService;
    private final ApplicationParams applicationParams;

    @Autowired
    public ClassifiedCreateEventOperation(@Qualifier("default") CreateEventOperation createEventOperation,
                                          SecurityClassificationServiceImpl classificationService,
                                          ApplicationParams applicationParams) {

        this.createEventOperation = createEventOperation;
        this.classificationService = classificationService;
        this.applicationParams = applicationParams;
    }

    @Override
    public CaseDetails createCaseEvent(String caseReference,
                                       CaseDataContent content) {
        final CaseDetails caseDetails = createEventOperation.createCaseEvent(caseReference,
                                                                           content);
        return applicationParams.isPocFeatureEnabled()
                ? caseDetails
                : Optional.ofNullable(caseDetails)
                       .flatMap(classificationService::applyClassification)
                       .orElse(null);
    }

    @Override
    public CaseDetails createCaseSystemEvent(String caseReference, Integer version,
                                             String attributePath, String categoryId) {
        final CaseDetails caseDetails = createEventOperation.createCaseSystemEvent(caseReference,
            version, attributePath, categoryId);
        return Optional.ofNullable(caseDetails)
            .flatMap(classificationService::applyClassification)
            .orElse(null);
    }
}
