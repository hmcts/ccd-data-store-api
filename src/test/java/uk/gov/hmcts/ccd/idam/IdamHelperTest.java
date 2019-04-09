package uk.gov.hmcts.ccd.idam;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.OAuth2Params;

import java.util.Base64;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.idam.IdamHelper.AUTHORIZATION_CODE;

class IdamHelperTest {

    @Mock
    private OAuth2Params oAuth2Params;
    @Mock
    private IdamApi idamApi;
    @Mock
    private IdamApiProvider idamApiProvider;
    @Mock
    private IdamApi.AuthenticateUserResponse authenticateUserResponse;
    @Mock
    private IdamApi.TokenExchangeResponse tokenExchangeResponse;
    @Mock
    private IdamApi.IdamUser idamUser;

    private IdamHelper idamHelper;
    private String email = "email";
    private String password = "password";
    private String code = "code";
    private String accessToken = "token";
    private String userId = "id";
    private List<String> roles = Lists.newArrayList("role1", "role2");
    private String idamBaseUrl = "idamBaseUrl";
    private String oauth2ClientId = "oauth2ClientId";
    private String oauth2ClientSecret = "oauth2ClientSecret";
    private String oauth2RedirectUrl = "oauth2RedirectUrl";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(idamBaseUrl).when(oAuth2Params).getIdamBaseURL();
        doReturn(oauth2ClientId).when(oAuth2Params).getOauth2ClientId();
        doReturn(oauth2ClientSecret).when(oAuth2Params).getOauth2ClientSecret();
        doReturn(oauth2RedirectUrl).when(oAuth2Params).getOauth2RedirectUrl();
        doReturn(idamApi).when(idamApiProvider).provide();
        doReturn(authenticateUserResponse).when(idamApi).authenticateUser(anyString(), anyString(), anyString(), anyString());
        doReturn(code).when(authenticateUserResponse).getCode();
        doReturn(tokenExchangeResponse).when(idamApi).exchangeCode(anyString(), anyString(), anyString(), anyString(), anyString());
        doReturn(accessToken).when(tokenExchangeResponse).getAccessToken();
        doReturn(idamUser).when(idamApi).getUser(eq(accessToken));
        doReturn(userId).when(idamUser).getId();
        doReturn(roles).when(idamUser).getRoles();

        idamHelper = new IdamHelper(oAuth2Params, idamApiProvider);
    }

    @Test
    @DisplayName("should authenticate if not done so already")
    void shouldAuthenticateIfNotDoneSoAlready() {

        AuthenticatedUser authenticate = idamHelper.authenticate(email, password);

        String expectedBase64Authorization = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());
        assertAll(
            () -> verify(idamApi).authenticateUser(eq("Basic " + expectedBase64Authorization), eq(IdamHelper.CODE), eq(oauth2ClientId), eq(oauth2RedirectUrl)),
            () -> verify(idamApi).exchangeCode(eq(code), eq(AUTHORIZATION_CODE), eq(oauth2ClientId), eq(oauth2ClientSecret), eq(oauth2RedirectUrl)),
            () -> verify(idamApi).getUser(eq(accessToken)),
            () -> assertThat(authenticate.getAccessToken(), is(accessToken)),
            () -> assertThat(authenticate.getId(), is(userId)),
            () -> assertThat(authenticate.getEmail(), is(email)),
            () -> assertThat(authenticate.getRoles(), is(roles))
        );
    }
}
