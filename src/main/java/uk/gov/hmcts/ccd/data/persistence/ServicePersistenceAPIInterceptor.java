package uk.gov.hmcts.ccd.data.persistence;



import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.ccd.data.SecurityUtils;


@RequiredArgsConstructor
class ServicePersistenceAPIInterceptor implements RequestInterceptor {

    private final SecurityUtils securityUtils;

    @Override
    public void apply(RequestTemplate template) {
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
