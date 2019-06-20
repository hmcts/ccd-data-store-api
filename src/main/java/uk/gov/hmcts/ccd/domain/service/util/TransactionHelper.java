package uk.gov.hmcts.ccd.domain.service.util;

import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.function.Supplier;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

@Component
public class TransactionHelper {

    @Transactional(value = REQUIRES_NEW)
    public <T> T withNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }
}
