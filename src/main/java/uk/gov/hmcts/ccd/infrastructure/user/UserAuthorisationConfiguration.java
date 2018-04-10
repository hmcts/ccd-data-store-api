package uk.gov.hmcts.ccd.infrastructure.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

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
        return new UserAuthorisation(getUserId(), getAccessLevel());
    }

    private ServiceAndUserDetails getServiceAndUserDetails() {
        return (ServiceAndUserDetails) SecurityContextHolder.getContext()
                                                            .getAuthentication()
                                                            .getPrincipal();
    }

    private String getUserId() {
        return getServiceAndUserDetails().getUsername();
    }

    private AccessLevel getAccessLevel() {
        return caseAccessService.getAccessLevel(getServiceAndUserDetails());
    }
}
