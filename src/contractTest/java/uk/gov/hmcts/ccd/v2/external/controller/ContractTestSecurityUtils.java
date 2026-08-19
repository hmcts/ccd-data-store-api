package uk.gov.hmcts.ccd.v2.external.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.TokenRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import java.util.HashMap;

import uk.gov.hmcts.ccd.security.idam.HmctsAccessApi;

@Service
@Slf4j
@Primary
@Profile("SECURITY_MOCK")
public class ContractTestSecurityUtils extends SecurityUtils {

    @Value("${auth.provider.client.redirect}")
    private String authRedirectUrl;

    @Value("${auth.provider.client.id}")
    private String authClientId;

    @Value("${auth.provider.client.secret}")
    private String authClientSecret;

    @Value("${auth.provider.client.scope}")
    private String authScope;

    private static final String GRANT_TYPE = "password";

    private HashMap<String, UserCredentials> caseTypeUserCredentials = new HashMap<>();
    private HashMap<String, UserCredentials> eventUserCredentials = new HashMap<>();

    private final HmctsAccessApi hmctsAccessApi;

    @Autowired
    public ContractTestSecurityUtils(AuthTokenGenerator authTokenGenerator,
                                     IdamRepository idamRepository, HmctsAccessApi hmctsAccessApi) {
        super(authTokenGenerator, idamRepository);
        this.hmctsAccessApi = hmctsAccessApi;
    }

    @Override
    public String getUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public String getUserToken() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }


    public void setSecurityContextUserAsCaseworkerForEvent(String eventId) {
        UserCredentials userCredentials = eventUserCredentials.get(eventId);
        setAuthenticationOnSecurityContext(userCredentials.username, userCredentials.password);
    }


    public void setSecurityContextUserAsCaseworkerByEvent(String eventId, String caseworkerUserName,
                                                          String caseworkerPassword) {
        setAuthenticationOnSecurityContext(caseworkerUserName, caseworkerPassword);
        eventUserCredentials.put(eventId, new UserCredentials(caseworkerUserName, caseworkerPassword));
    }

    public void setSecurityContextUserAsCaseworkerForCaseType(String caseType) {
        UserCredentials userCredentials = caseTypeUserCredentials.get(caseType);
        setAuthenticationOnSecurityContext(userCredentials.username, userCredentials.password);
    }


    public void setSecurityContextUserAsCaseworkerByCaseType(String caseType, String caseworkerUserName,
                                                          String caseworkerPassword) {
        setAuthenticationOnSecurityContext(caseworkerUserName, caseworkerPassword);
        caseTypeUserCredentials.put(caseType, new UserCredentials(caseworkerUserName, caseworkerPassword));
    }

    private void setAuthenticationOnSecurityContext(String caseworkerUserName, String caseworkerPassword) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(caseworkerUserName, getCaseworkerToken(caseworkerUserName,
                    caseworkerPassword)));
    }

    private String getCaseworkerToken(String caseworkerUserName, String caseworkerPassword) {
        return getIdamOauth2Token(caseworkerUserName, caseworkerPassword);
    }

    private String getIdamOauth2Token(String username, String password) {

        log.info("Client ID: {} . Authenticating...", authClientId);

        log.info("Authenticated. Exchanging...");
        TokenResponse tokenExchangeResponse = hmctsAccessApi.generateOpenIdToken(
            new TokenRequest(authClientId, authClientSecret, GRANT_TYPE, authRedirectUrl, username, password, authScope,
                null, null)
        );


        log.info("Getting AccessToken...");
        return tokenExchangeResponse.accessToken;
    }

    class UserCredentials {
        private final String username;
        private final String password;

        UserCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
