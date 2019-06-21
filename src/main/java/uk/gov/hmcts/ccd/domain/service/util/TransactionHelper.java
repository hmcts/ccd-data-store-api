package uk.gov.hmcts.ccd.domain.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionHelper.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T withNewTransaction(Supplier<T> supplier) {
        LOG.info("inside TransactionHelper");
        return supplier.get();
    }
}
