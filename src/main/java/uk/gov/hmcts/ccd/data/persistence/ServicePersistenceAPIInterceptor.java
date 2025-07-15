package uk.gov.hmcts.ccd.data.persistence;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@RequiredArgsConstructor
public class ServicePersistenceAPIInterceptor implements RequestInterceptor {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyKeyHolder idempotencyKeyHolder;
    private final SecurityUtils securityUtils;

    @Override
    public void apply(RequestTemplate template) {
        UUID idempotencyKey = idempotencyKeyHolder.getKey();

        if (idempotencyKey != null) {
            template.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey.toString());
        }

        if (!template.headers().containsKey(AUTHORIZATION)) {
            template.header(AUTHORIZATION, securityUtils.getUserBearerToken());
        }
        if (!template.headers().containsKey(SERVICE_AUTHORIZATION)) {
            template.header(SERVICE_AUTHORIZATION, securityUtils.getServiceAuthorization());
        }

        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        var request = requestAttributes.getRequest();
        template.header(HttpHeaders.REFERER, request.getHeader(HttpHeaders.REFERER));

    }
}
