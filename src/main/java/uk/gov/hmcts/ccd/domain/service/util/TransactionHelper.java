package uk.gov.hmcts.ccd.domain.service.util;

import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.function.Supplier;

@Component
public class TransactionHelper {

    @Transactional()
    public <T> T withNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }
}
