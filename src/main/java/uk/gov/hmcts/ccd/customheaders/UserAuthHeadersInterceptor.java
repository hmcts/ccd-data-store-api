package uk.gov.hmcts.ccd.customheaders;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import uk.gov.hmcts.ccd.data.SecurityUtils;

public class UserAuthHeadersInterceptor implements RequestInterceptor {

    private static final String EXPERIMENTAL = "experimental";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private final SecurityUtils securityUtils;

    public UserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (!template.headers().containsKey(AUTHORIZATION)) {
            template.header(AUTHORIZATION, securityUtils.getUserBearerToken());
        }
        if (!template.headers().containsKey(SERVICE_AUTHORIZATION)) {
            template.header(SERVICE_AUTHORIZATION, securityUtils.getServiceAuthorization());
        }
        // TODO: will be removed once ccd cleaned in their end
        template.header(EXPERIMENTAL, "true");
    }
}
