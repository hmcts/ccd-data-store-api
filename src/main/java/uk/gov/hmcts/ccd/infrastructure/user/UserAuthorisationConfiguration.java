package uk.gov.hmcts.ccd.infrastructure.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class UserAuthorisationConfiguration {

    private final CaseAccessService caseAccessService;
    private final SecurityUtils securityUtils;

    @Autowired
    public UserAuthorisationConfiguration(CaseAccessService caseAccessService, SecurityUtils securityUtils) {
        this.caseAccessService = caseAccessService;
        this.securityUtils = securityUtils;
    }

    @Bean
    @RequestScope
    public UserAuthorisation create() {
        UserInfo userInfo = securityUtils.getUserInfo();
        return new UserAuthorisation(userInfo.getUid(),
                                     getAccessLevel(userInfo),
                                     getRoles(userInfo));
    }

    private AccessLevel getAccessLevel(UserInfo userInfo) {
        return caseAccessService.getAccessLevel(userInfo);
    }

    private Set<String> getRoles(UserInfo userInfo) {
        return new HashSet<String>(userInfo.getRoles());
    }
}
