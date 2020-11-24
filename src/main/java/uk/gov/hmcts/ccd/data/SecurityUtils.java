package uk.gov.hmcts.ccd.data;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import uk.gov.hmcts.ccd.security.idam.IdamRepository;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.models.AuthenticateUserRequest;
import uk.gov.hmcts.reform.idam.client.models.AuthenticateUserResponse;
import uk.gov.hmcts.reform.idam.client.models.ExchangeCodeRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenExchangeResponse;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

@Service
@Slf4j
public class SecurityUtils {
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String AUD_CLAIM = "aud";
    private static final String BEARER = "Bearer ";
    private static final String BASIC = "Basic ";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";

    @Value("${auth.provider.client.redirect}")
    private String authRedirectUrl;

    @Value("${auth.provider.client.id}")
    private String authClientId;

    @Value("${auth.provider.client.secret}")
    private String authClientSecret;

    @Value("${auth.provider.caseworker.email}")
    private String caseworkerUserName;

    @Value("${auth.provider.caseworker.password}")
    private String caseworkerPassword;



    private final AuthTokenGenerator authTokenGenerator;
    private final IdamRepository idamRepository;
    private final IdamApi idamClient;

    @Autowired
    public SecurityUtils(final AuthTokenGenerator authTokenGenerator, IdamRepository idamRepository, IdamApi idamApi) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamRepository = idamRepository;
        this.idamClient = idamApi;
    }

    public HttpHeaders authorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(SERVICE_AUTHORIZATION, authTokenGenerator.generate());
        headers.add("user-id", getUserId());
        headers.add("user-roles", getUserRolesHeader());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            headers.add(HttpHeaders.AUTHORIZATION, getUserBearerToken());
        }
        return headers;
    }

    public HttpHeaders userAuthorizationHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getUserBearerToken());
        return headers;
    }

    public UserInfo getUserInfo() {
        return idamRepository.getUserInfo(getUserToken());
    }

    public String getUserId() {
        return getUserInfo().getUid();
    }

    public String getUserToken() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getTokenValue();
    }

    public boolean isAuthenticated() {
        return Objects.nonNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private String getUserBearerToken() {
        return BEARER + getUserToken();
    }

    public String getUserRolesHeader() {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream()
                             .map(GrantedAuthority::getAuthority)
                             .collect(Collectors.joining(","));
    }

    public String getServiceName() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> claims = jwt.getClaimAsStringList(AUD_CLAIM);
        return CollectionUtils.isNotEmpty(claims) ? claims.get(0) : null;
    }

    public String getServiceNameFromS2SToken(String serviceAuthenticationToken) {
        // NB: this grabs the servce name straight from the token under the assumption
        // that the S2S token has already been verified elsewhere
        return JWT.decode(removeBearerFromToken(serviceAuthenticationToken)).getSubject();
    }

    private String removeBearerFromToken(String token) {
        if (!token.startsWith(BEARER)) {
            return token;
        } else {
            return token.substring(BEARER.length());
        }
    }


    public void setSecurityContextUserAsCaseworker() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(caseworkerUserName, getCaseworkerToken()));
    }

    private String getCaseworkerToken() {
        return getIdamOauth2Token(caseworkerUserName, caseworkerPassword);
    }

    private String getIdamOauth2Token(String username, String password) {
        String basicAuthHeader = getBasicAuthHeader(username, password);

        log.info("Client ID: {} . Authenticating...", authClientId);

        AuthenticateUserResponse authenticateUserResponse = idamClient.authenticateUser(
            basicAuthHeader, new AuthenticateUserRequest(CODE, authClientId, authRedirectUrl)
        );

        log.info("Authenticated. Exchanging...");
        TokenExchangeResponse tokenExchangeResponse = idamClient.exchangeCode(new ExchangeCodeRequest(authenticateUserResponse.getCode(),
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
}
