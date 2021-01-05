package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.DefaultGetCaseOperation;

import java.util.Optional;

@Service
@Primary
@Qualifier(CreatorGetCaseOperation.QUALIFIER)
@Profile("SECURITY_MOCK")
public class ContractTestGetCaseOperation extends DefaultGetCaseOperation {

    private String testCaseReference;
    private final ContractTestSecurityUtils contractTestSecurityUtils;


    @Autowired
    public ContractTestGetCaseOperation(@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                            CaseDetailsRepository caseDetailsRepository,
                                        UIDService uidService, ContractTestSecurityUtils contractTestSecurityUtils) {
        super(caseDetailsRepository, uidService);
        this.contractTestSecurityUtils = contractTestSecurityUtils;

    }

    @Override
    public Optional<CaseDetails> execute(final String jurisdictionId, final String caseTypeId,
                                         final String caseReference) {
        contractTestSecurityUtils.setSecurityContextUserAsCaseworkerForCaseType(caseTypeId);
        return super.execute(jurisdictionId, caseTypeId, testCaseReference);
    }

    @Override
    public Optional<CaseDetails> execute(String caseReference) {
        return super.execute(testCaseReference);
    }

    public void setTestCaseReference(String testCaseReference) {
        this.testCaseReference = testCaseReference;
    }
}
