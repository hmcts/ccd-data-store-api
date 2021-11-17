package uk.gov.hmcts.ccd.domain.service.getcase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.Optional;

@Service
@Qualifier(CreatorGetCaseOperation.QUALIFIER)
@Slf4j
public class CreatorGetCaseOperation implements GetCaseOperation {

    public static final String QUALIFIER = "creator";

    private GetCaseOperation getCaseOperation;

    private CaseAccessService caseAccessService;

    private ApplicationParams applicationParams;

    public CreatorGetCaseOperation(@Qualifier("authorised") GetCaseOperation getCaseOperation,
                                   CaseAccessService caseAccessService,
                                   ApplicationParams applicationParams) {
        this.getCaseOperation = getCaseOperation;
        this.caseAccessService = caseAccessService;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        log.info("Get case operation Creator Jurisdiction {}  CaseTypeId {}, CaseReference {}",
            jurisdictionId, caseTypeId, caseReference);
        return this.getCaseOperation.execute(jurisdictionId, caseTypeId, caseReference)
            .flatMap(this::checkVisibility);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        log.info("Get case operation Creator Jurisdiction {} ", caseReference);
        return this.getCaseOperation.execute(caseReference)
            .flatMap(this::checkVisibility);
    }

    private Optional<CaseDetails> checkVisibility(CaseDetails caseDetails) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return Optional.of(caseDetails);
        }
        return this.caseAccessService.canUserAccess(caseDetails)
            ? Optional.of(caseDetails) : Optional.empty();
    }

}
