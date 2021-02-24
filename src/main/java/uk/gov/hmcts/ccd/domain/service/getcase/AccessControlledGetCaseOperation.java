package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import java.util.Optional;

@Service
@Qualifier(AccessControlledGetCaseOperation.QUALIFIER)
public class AccessControlledGetCaseOperation implements GetCaseOperation, AccessControl {
    public static final String QUALIFIER = "access-control";

    private final GetCaseOperation getCaseOperation;
    private final GetCaseOperation creatorGetCaseOperation;
    private final CaseDataAccessControl caseDataAccessControl;
    private final ApplicationParams applicationParams;

    @Autowired
    public AccessControlledGetCaseOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER)
                                                    GetCaseOperation creatorGetCaseOperation,
                                            @Qualifier("default") GetCaseOperation getCaseOperation,
                                            CaseDataAccessControl caseDataAccessControl,
                                            ApplicationParams applicationParams) {
        this.getCaseOperation = getCaseOperation;
        this.creatorGetCaseOperation = creatorGetCaseOperation;
        this.caseDataAccessControl = caseDataAccessControl;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return this.execute(caseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            return getCaseOperation.execute(caseReference)
                .flatMap(caseDataAccessControl::applyAccessControl);
        } else {
            return creatorGetCaseOperation.execute(caseReference);
        }
    }
}
