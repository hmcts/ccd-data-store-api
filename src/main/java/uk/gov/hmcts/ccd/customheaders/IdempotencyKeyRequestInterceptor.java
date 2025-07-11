package uk.gov.hmcts.ccd.customheaders;

import java.util.UUID;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

public class IdempotencyKeyRequestInterceptor implements RequestInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyKeyHolder idempotencyKeyHolder;

    public IdempotencyKeyRequestInterceptor(IdempotencyKeyHolder idempotencyKeyHolder) {
        this.idempotencyKeyHolder = idempotencyKeyHolder;
    }

    @Override
    public void apply(RequestTemplate template) {
        UUID idempotencyKey = idempotencyKeyHolder.getKey();

        if (idempotencyKey != null) {
            template.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey.toString());
        }
    }
}
