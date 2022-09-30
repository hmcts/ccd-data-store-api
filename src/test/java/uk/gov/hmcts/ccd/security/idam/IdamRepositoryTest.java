package uk.gov.hmcts.ccd.security.idam;

import feign.FeignException;
import feign.Request;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class IdamRepositoryTest {

    private static final String TEST_USER_TOKEN = "TestUserToken";
    private static final String USER_ID = "232-SFWE-4543-CVDSF";
    private static final List<String> ROLES = Arrays.asList("role1", "role2");

    @Mock
    private IdamClient idamClient;
    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private IdamRepository idamRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(idamRepository, "selfInstance", idamRepository);
    }

    @Test
    @DisplayName("Get user info if token is passed")
    void shouldGetUserInfo() {
        UserInfo userInfo = UserInfo.builder().build();
        given(idamClient.getUserInfo("Bearer " + TEST_USER_TOKEN)).willReturn(userInfo);
        UserInfo result = idamRepository.getUserInfo(TEST_USER_TOKEN);
        assertThat(result).isSameAs(userInfo);
    }

    @Test
    @DisplayName("Return InvalidTokenException when get user info Unauthorized")
    void shouldReturnUnauthorizedGetUserInfo() {
        FeignException.Unauthorized exception = new FeignException
            .Unauthorized("myUniqueExceptionMessage",
            Request.create(Request.HttpMethod.GET, "myUniqueExceptionMessage", Map.of(), new byte[0],
                Charset.defaultCharset(), null), new byte[0], new HashMap<>());

        given(idamClient.getUserInfo("Bearer " + TEST_USER_TOKEN)).willThrow(exception);
        Assert.assertThrows(InvalidTokenException.class, () -> idamRepository.getUserInfo(TEST_USER_TOKEN));
    }

    @Test
    @DisplayName("Return InvalidTokenException when get user info returns BadRequest")
    void shouldInternalServerErrorGetUserInfoWithBadRequest() {
        FeignException.BadRequest exception = new FeignException
            .BadRequest("myUniqueExceptionMessage",
            Request.create(Request.HttpMethod.GET, "myUniqueExceptionMessage", Map.of(), new byte[0],
                Charset.defaultCharset(), null), new byte[0], new HashMap<>());
        given(idamClient.getUserInfo("Bearer " + TEST_USER_TOKEN)).willThrow(exception);
        Assert.assertThrows(InvalidTokenException.class, () -> idamRepository.getUserInfo(TEST_USER_TOKEN));
    }

    @Test
    @DisplayName("Return ServiceException when get user info returns InternalServerError")
    void shouldBadGatewayGetUserInfo() {
        FeignException.InternalServerError exception = new FeignException
            .InternalServerError("myUniqueExceptionMessage",
            Request.create(Request.HttpMethod.GET, "myUniqueExceptionMessage", Map.of(), new byte[0],
                Charset.defaultCharset(), null), new byte[0], new HashMap<>());
        given(idamClient.getUserInfo("Bearer " + TEST_USER_TOKEN)).willThrow(exception);
        Assert.assertThrows(ServiceException.class, () -> idamRepository.getUserInfo(TEST_USER_TOKEN));
    }

    @Test
    @DisplayName("ServiceUnavailable get user info")
    void shouldServiceUnavailableGetUserInfo() {
        FeignException.ServiceUnavailable exception = new FeignException
            .ServiceUnavailable("myUniqueExceptionMessage",
            Request.create(Request.HttpMethod.GET, "myUniqueExceptionMessage", Map.of(), new byte[0],
                Charset.defaultCharset(), null), new byte[0], new HashMap<>());
        given(idamClient.getUserInfo("Bearer " + TEST_USER_TOKEN)).willThrow(exception);
        Assert.assertThrows(ServiceException.class,
            () -> idamRepository.getUserInfo(TEST_USER_TOKEN));
    }

    @Test
    @DisplayName("Get User Roles by UserId")
    void shouldGetUserRolesByUserId() {
        mockGetDataStoreSystemUserAccessToken();
        mockGetUserByUserId();

        List<String> idamUserRoles = idamRepository.getUserRoles(USER_ID);
        assertThat(idamUserRoles).isSameAs(ROLES);
    }

    @Test
    @DisplayName("Get DataStore's SystemUser access token")
    void shouldGetDataStoreSystemUserAccessToken() {

        // GIVEN
        mockGetDataStoreSystemUserAccessToken();

        // WHEN
        String token = idamRepository.getDataStoreSystemUserAccessToken();

        // THEN
        assertThat(token).isSameAs(TEST_USER_TOKEN);

    }

    private void mockGetUserByUserId() {
        UserDetails userDetails = UserDetails.builder()
            .id(USER_ID)
            .roles(ROLES)
            .build();
        given(idamClient.getUserByUserId(TEST_USER_TOKEN, USER_ID)).willReturn(userDetails);
    }

    private void mockGetDataStoreSystemUserAccessToken() {
        String userId = "TestSystemUser";
        String password = "aPassword";
        given(applicationParams.getDataStoreSystemUserId()).willReturn(userId);
        given(applicationParams.getDataStoreSystemUserPassword()).willReturn(password);
        given(idamClient.getAccessToken(userId, password)).willReturn(TEST_USER_TOKEN);
    }
}
