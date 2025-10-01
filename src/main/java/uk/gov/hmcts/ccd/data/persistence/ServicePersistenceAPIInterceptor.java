package uk.gov.hmcts.ccd.data.persistence;



import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.ccd.data.SecurityUtils;


@RequiredArgsConstructor
class ServicePersistenceAPIInterceptor implements RequestInterceptor {

    private final SecurityUtils securityUtils;

    @Override
    public void apply(RequestTemplate template) {
        template.header(AUTHORIZATION, securityUtils.getUserBearerToken());
        template.header(SERVICE_AUTHORIZATION, securityUtils.getServiceAuthorization());
    }
}
