package uk.gov.hmcts.ccd.domain.service.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;


@Component
/**
 * Helper class to provide spring transaction aspect on private methods.
 */
public class TransactionHelper {

    @Transactional(propagation = REQUIRES_NEW)
    public <T> T withNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void withNewTransaction(Runnable runnable) {
        runnable.run();
    }
}
