package uk.gov.hmcts.ccd.domain.service.getcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataAccessControl;

import java.util.Optional;

@Service
@Qualifier(AccessControlledGetCaseOperation.QUALIFIER)
public class AccessControlledGetCaseOperation implements GetCaseOperation {
    public static final String QUALIFIER = "access-control";

    private final GetCaseOperation getCaseOperation;
    private final GetCaseOperation creatorGetCaseOperation;
    private final CaseDataAccessControl defaultCaseDataAccessControl;
    private final ApplicationParams applicationParams;

    @Autowired
    public AccessControlledGetCaseOperation(@Qualifier(CreatorGetCaseOperation.QUALIFIER)
                                                    GetCaseOperation creatorGetCaseOperation,
                                            @Qualifier("default") GetCaseOperation getCaseOperation,
                                            CaseDataAccessControl defaultCaseDataAccessControl,
                                            ApplicationParams applicationParams) {
        this.getCaseOperation = getCaseOperation;
        this.creatorGetCaseOperation = creatorGetCaseOperation;
        this.defaultCaseDataAccessControl = defaultCaseDataAccessControl;
        this.applicationParams = applicationParams;
    }

    @Override
    public Optional<CaseDetails> execute(String jurisdictionId, String caseTypeId, String caseReference) {
        return this.execute(caseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        if (applicationParams.getCcdNewAccessControlEnabled()) {
            return getCaseOperation.execute(caseReference)
                .flatMap(defaultCaseDataAccessControl::applyAccessControl);
        } else {
            return creatorGetCaseOperation.execute(caseReference);
        }
    }
}
