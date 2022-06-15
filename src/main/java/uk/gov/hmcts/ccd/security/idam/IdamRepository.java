package uk.gov.hmcts.ccd.security.idam;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.ccd.ApplicationParams;
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
            UserInfo userInfo = idamClient.getUserInfo("Bearer " + jwtToken);
            if (userInfo != null) {
                log.info("Queried user info from IDAM API. User Id={}. Roles={}.",
                    userInfo.getUid(), userInfo.getRoles());
            }
            return userInfo;
        } catch (FeignException feignException) {
            log.error("FeignException: retrieve user info ", feignException);
            HttpStatus httpStatus = getHttpStatus(feignException);
            throw new ResponseStatusException(httpStatus, "error while retrieving user info", feignException);
        }
    }

    @Cacheable("idamUserRoleCache")
    public List<String> getUserRoles(String userId) {
        String dataStoreSystemUserToken = selfInstance.getDataStoreSystemUserAccessToken();
        List<String> roles = getUserByUserId(userId, dataStoreSystemUserToken).getRoles();
        log.debug("System user queried user info from IDAM API. User Id={}. Roles={}.", userId, roles);
        return roles;
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

    private HttpStatus getHttpStatus(FeignException exception) {
        HttpStatus httpStatus = HttpStatus.valueOf(exception.status());

        if (exception instanceof FeignException.FeignClientException) {
            if (httpStatus != HttpStatus.UNAUTHORIZED) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        } else if (exception instanceof FeignException.FeignServerException) {
            if (httpStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
                httpStatus = HttpStatus.BAD_GATEWAY;
            }
        }

        return httpStatus;
    }
}
