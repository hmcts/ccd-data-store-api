package uk.gov.hmcts.ccd.v2.external.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.models.AuthenticateUserRequest;
import uk.gov.hmcts.reform.idam.client.models.AuthenticateUserResponse;
import uk.gov.hmcts.reform.idam.client.models.ExchangeCodeRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenExchangeResponse;

import java.util.Base64;
import java.util.HashMap;

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

    private static final String BASIC = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";

    private HashMap<String, UserCredentials> caseTypeUserCredentials = new HashMap<>();
    private HashMap<String, UserCredentials> eventUserCredentials = new HashMap<>();

    private final IdamApi idamClient;

    @Autowired
    public ContractTestSecurityUtils(AuthTokenGenerator authTokenGenerator,
                                     IdamRepository idamRepository, IdamApi idamApi) {
        super(authTokenGenerator, idamRepository);
        this.idamClient = idamApi;
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
    //    SecurityContextHolder.getContext()
    //        .setAuthentication(
    //            new UsernamePasswordAuthenticationToken(caseworkerUserName, getCaseworkerToken(caseworkerUserName,
    //                caseworkerPassword)));
    }

    private String getCaseworkerToken(String caseworkerUserName, String caseworkerPassword) {
        return getIdamOauth2Token(caseworkerUserName, caseworkerPassword);
    }

    private String getIdamOauth2Token(String username, String password) {
        String basicAuthHeader = getBasicAuthHeader(username, password);

        log.info("Client ID: {} . Authenticating...", authClientId);

        AuthenticateUserResponse authenticateUserResponse = idamClient.authenticateUser(
            basicAuthHeader, new AuthenticateUserRequest(CODE, authClientId, authRedirectUrl)
        );

        log.info("Authenticated. Exchanging...");
        TokenExchangeResponse tokenExchangeResponse = idamClient.exchangeCode(
            new ExchangeCodeRequest(authenticateUserResponse.getCode(),
                AUTHORIZATION_CODE,
                authRedirectUrl,
                authClientId,
                authClientSecret));

        log.info("Getting AccessToken...");
        return tokenExchangeResponse.getAccessToken();
    }

    private String getBasicAuthHeader(String username, String password) {
        String authorisation = username + ":" + password;
        return BASIC + Base64.getEncoder().encodeToString(authorisation.getBytes());
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
