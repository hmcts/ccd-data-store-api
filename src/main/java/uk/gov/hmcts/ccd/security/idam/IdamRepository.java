package uk.gov.hmcts.ccd.security.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.List;

@Component
@Slf4j
public class IdamRepository {

    private final IdamClient idamClient;
    private final ApplicationParams applicationParams;

    @Autowired
    public IdamRepository(IdamClient idamClient, ApplicationParams applicationParams) {
        this.idamClient = idamClient;
        this.applicationParams = applicationParams;
    }

    @Cacheable(value = "userInfoCache")
    public UserInfo getUserInfo(String jwtToken) {
        UserInfo userInfo = idamClient.getUserInfo("Bearer " + jwtToken);
        if (userInfo != null) {
            log.info("Queried user info from IDAM API. User Id={}. Roles={}.", userInfo.getUid(), userInfo.getRoles());
        }
        return userInfo;
    }

    @Cacheable("idamUserRoleCache")
    public List<String> getUserRoles(String userId) {
        String dataStoreSystemUserToken = getDataStoreSystemUserAccessToken();
        List<String> roles = getUserByUserId(userId, dataStoreSystemUserToken).getRoles();
        log.debug("System user queried user info from IDAM API. User Id={}. Roles={}.", userId, roles);
        return roles;
    }

    private UserDetails getUserByUserId(String userId, String bearerToken) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }

    private String getDataStoreSystemUserAccessToken() {
        return idamClient.getAccessToken(applicationParams.getDataStoreSystemUserId(),
            applicationParams.getDataStoreSystemUserPassword());
    }
}
