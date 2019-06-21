package uk.gov.hmcts.ccd.domain.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.util.function.Supplier;

@Component
public class TransactionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionHelper.class);
    private final CaseDetailsRepository caseDetailsRepository;

    @Inject
    public TransactionHelper(@Qualifier(DefaultCaseDetailsRepository.QUALIFIER) CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T withNewTransaction(Supplier<T> supplier) {
        LOG.info("inside TransactionHelper");
        return supplier.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CaseDetails commitCaseDetails(CaseDetails caseDetails) {
        LOG.info("inside TransactionHelper caseDetailsRepository");
        return caseDetailsRepository.set(caseDetails);
    }
}
