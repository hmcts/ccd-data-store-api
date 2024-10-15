package uk.gov.hmcts.ccd.security.idam;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class IdamRepository {

    private final IdamClient idamClient;
    private final ApplicationParams applicationParams;

    @Resource(name = "idamRepository")
    private IdamRepository selfInstance;

    @Autowired
    public IdamRepository(IdamClient idamClient, ApplicationParams applicationParams) {
        this.idamClient = idamClient;
        this.applicationParams = applicationParams;
    }

    @Cacheable(value = "userInfoCache")
    public UserInfo getUserInfo(String jwtToken) {
        try {
            return idamClient.getUserInfo("Bearer " + jwtToken);
        } catch (FeignException exception) {
            log.error("FeignException: retrieve user info: {} ", exception.getMessage());

            if (isClientError(exception)) {
                throw new InvalidTokenException(exception.getMessage(), exception);
            } else {
                throw new ServiceException(exception.getMessage(), exception);
            }
        }
    }

    private boolean isClientError(FeignException exception) {
        return exception.status() >= 400 && exception.status() <= 499;
    }

    @Cacheable("idamUserRoleCache")
    public List<String> getUserRoles(String userId) {
        String dataStoreSystemUserToken = selfInstance.getDataStoreSystemUserAccessToken();
        List<String> roles = getUserByUserId(userId, dataStoreSystemUserToken).getRoles();
        log.debug("System user queried user info from IDAM API. User Id={}. Roles={}.", userId, roles);
        return roles;
    }

    @Cacheable("idamUserByIdCache")
    public UserDetails getUserByUserId(String userId) {
        String dataStoreSystemUserToken = selfInstance.getDataStoreSystemUserAccessToken();
        return getUserByUserId(userId, dataStoreSystemUserToken);
    }

    private UserDetails getUserByUserId(String userId, String bearerToken) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }

    @Cacheable("systemUserTokenCache")
    public String getDataStoreSystemUserAccessToken() {
        log.info("Getting a fresh token for system account.");
        return idamClient.getAccessToken(applicationParams.getDataStoreSystemUserId(),
            applicationParams.getDataStoreSystemUserPassword());
    }
}
