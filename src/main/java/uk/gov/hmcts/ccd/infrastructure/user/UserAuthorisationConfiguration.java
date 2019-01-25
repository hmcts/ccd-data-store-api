package uk.gov.hmcts.ccd.infrastructure.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class UserAuthorisationConfiguration {

    private final CaseAccessService caseAccessService;

    @Autowired
    public UserAuthorisationConfiguration(CaseAccessService caseAccessService) {
        this.caseAccessService = caseAccessService;
    }

    @Bean
    @RequestScope
    public UserAuthorisation create() {
        final ServiceAndUserDetails serviceAndUser = getServiceAndUserDetails();
        return new UserAuthorisation(getUserId(serviceAndUser),
                                     getAccessLevel(serviceAndUser),
                                     getRoles(serviceAndUser));
    }

    private ServiceAndUserDetails getServiceAndUserDetails() {
        return (ServiceAndUserDetails) SecurityContextHolder.getContext()
                                                            .getAuthentication()
                                                            .getPrincipal();
    }

    private String getUserId(ServiceAndUserDetails serviceAndUser) {
        return serviceAndUser.getUsername();
    }

    private AccessLevel getAccessLevel(ServiceAndUserDetails serviceAndUser) {
        return caseAccessService.getAccessLevel(serviceAndUser);
    }

    private Set<String> getRoles(ServiceAndUserDetails serviceAndUser) {
        return serviceAndUser.getAuthorities()
                             .stream()
                             .map(GrantedAuthority::getAuthority)
                             .collect(Collectors.toSet());
    }
}
