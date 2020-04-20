package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.Optional;

@Service
@Qualifier(CreatorGetCaseOperation.QUALIFIER)
public class CreatorGetCaseOperation implements GetCaseOperation {

    public static final String QUALIFIER = "creator";

    private GetCaseOperation getCaseOperation;

    private CaseAccessService caseAccessService;

    public CreatorGetCaseOperation(@Qualifier("authorised") GetCaseOperation getCaseOperation,
                                   CaseAccessService caseAccessService) {
        this.getCaseOperation = getCaseOperation;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return this.getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
            .flatMap(this::checkVisibility);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return this.getCaseOperation.execute(caseReference)
            .flatMap(this::checkVisibility);
    }

    private Optional<CaseDetails> checkVisibility(CaseDetails caseDetails) {
        return this.caseAccessService.canUserAccess(caseDetails)
            ? Optional.of(caseDetails) : Optional.empty();
    }

}
