package uk.gov.hmcts.ccd.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.auth.provider.service.token.ServiceTokenGenerator;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

@Service
public class SecurityUtils {
    private final ServiceTokenGenerator serviceTokenGenerator;

    @Autowired
    public SecurityUtils(@Qualifier("cachedServiceTokenGenerator") final ServiceTokenGenerator serviceTokenGenerator) {
        this.serviceTokenGenerator = serviceTokenGenerator;
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("ServiceAuthorization", serviceTokenGenerator.generate());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (serviceAndUser.getPassword() != null) {
                headers.add(HttpHeaders.AUTHORIZATION, serviceAndUser.getPassword());
            }
        }
        return headers;
    }

    public HttpHeaders userAuthorizationHeaders() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, serviceAndUser.getPassword());
        return headers;
    }

    public String getUserId() {
        final ServiceAndUserDetails serviceAndUser = (ServiceAndUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return serviceAndUser.getUsername();
    }
}
