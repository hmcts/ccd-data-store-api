package uk.gov.hmcts.ccd.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DefaultUserRepositoryTest {

    private static final String ROLE_CASEWORKER = "caseworker";
    private static final String ROLE_CASEWORKER_TEST = "caseworker-test";
    private static final String ROLE_CASEWORKER_CMC = "caseworker-cmc";
    private static final String ROLE_CASEWORKER_CAA = "caseworker-caa";
    private static final String JURISDICTION_ID = "CMC";
    private static final String CITIZEN = "citizen";
    private static final String PROBATE_PRIVATE_BETA = "probate-private-beta";
    private static final String LETTER_HOLDER = "letter-holder";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthCheckerConfiguration authCheckerConfiguration;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private DefaultUserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        initSecurityContext();

        mockUserInfo("userId");

        when(applicationParams.getCcdAccessControlCrossJurisdictionRoles())
            .thenReturn(singletonList(ROLE_CASEWORKER_CAA));
        when(applicationParams.getCcdAccessControlCaseworkerRoleRegex()).thenReturn("caseworker.+");
    }

    @Nested
    @DisplayName("getUserRoles()")
    class GetUserRoles {

        @Test
        @DisplayName("should return user roles")
        void shouldReturnUserRoles() {
            asCaseworker();

            final Set<String> roles = userRepository.getUserRoles();

            assertAll(
                () -> assertThat(roles, hasSize(3)),
                () -> assertThat(roles, hasItems(ROLE_CASEWORKER, ROLE_CASEWORKER_TEST, ROLE_CASEWORKER_CMC))
            );
        }
    }

    @Nested
    @DisplayName("getUserClassifications()")
    class GetUserClassifications {

        @Nested
        @DisplayName("when caseworker")
        class WhenCaseworker {

            @Test
            @DisplayName("should only consider roles for given jurisdiction")
            void shouldOnlyConsiderRolesForJurisdiction() {
                asCaseworker();

                userRepository.getUserClassifications(JURISDICTION_ID);

                assertAll(
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(
                        singletonList(ROLE_CASEWORKER_CMC)),
                    () -> verifyNoMoreInteractions(caseDefinitionRepository)
                );
            }
        }

        @Nested
        @DisplayName("when caseworker-caa")
        class WhenCaseworkerCaa {

            @Test
            @DisplayName("should only consider roles for given jurisdiction")
            void shouldOnlyConsiderRolesForJurisdiction() {
                asCaseworkerCaa();
                userRepository.getUserClassifications(JURISDICTION_ID);

                assertAll(
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(
                        singletonList(ROLE_CASEWORKER_CAA)),
                    () -> verifyNoMoreInteractions(caseDefinitionRepository)
                );
            }
        }

        @Nested
        @DisplayName("when citizen")
        class WhenCitizen {

            @Test
            @DisplayName("should consider `citizen` role")
            void shouldConsiderCitizen() {
                asCitizen();
                String[] roles = new String[2];
                roles[0] = "citizen";
                roles[1] = "letter-holder";
                when(authCheckerConfiguration.getCitizenRoles()).thenReturn(roles);
                userRepository.getUserClassifications(JURISDICTION_ID);

                assertAll(
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(singletonList(CITIZEN)),
                    () -> verifyNoMoreInteractions(caseDefinitionRepository)
                );
            }

            @Test
            @DisplayName("should consider `letter-holder` role")
            void shouldConsiderLetterHolder() {
                asLetterHolderCitizen();

                String[] roles = new String[2];
                roles[0] = "citizen";
                roles[1] = "letter-holder";
                when(authCheckerConfiguration.getCitizenRoles()).thenReturn(roles);
                userRepository.getUserClassifications(JURISDICTION_ID);

                assertAll(
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(
                        singletonList(LETTER_HOLDER)),
                    () -> verifyNoMoreInteractions(caseDefinitionRepository)
                );
            }
        }

        @Test
        @DisplayName("should exclude all unsupported roles")
        void shouldExcludeOtherRoles() {
            asOtherRoles();

            verifyNoMoreInteractions(caseDefinitionRepository);
        }
    }

    @Test
    void getUserDefaultSettingsShouldReturnServiceExceptionWhenMessageIsNull() {
        assertThrows(ServiceException.class, () -> {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            HttpClientErrorException response = createErrorResponse(HttpStatus.BAD_GATEWAY, null);
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(UserDefault.class))).thenThrow(response);
            doThrow(response).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), anyMap());

            userRepository.getUserDefaultSettings("222");
        });
    }

    @Test
    void getUserDefaultSettingsShouldReturnResourceNotFoundExceptionWhen404() {
        assertThrows(ResourceNotFoundException.class, () -> {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            HttpClientErrorException response = createErrorResponse(HttpStatus.NOT_FOUND, "some message");
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(UserDefault.class))).thenThrow(response);

            userRepository.getUserDefaultSettings("222");
        });
    }

    @Test
    void getUserDefaultSettingsShouldReturnBadRequestWhenNot404() {
        assertThrows(BadRequestException.class, () -> {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            HttpClientErrorException response = createErrorResponse(HttpStatus.BAD_GATEWAY, "some message");
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class),
                eq(UserDefault.class))).thenThrow(response);

            doThrow(response).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), anyMap());

            userRepository.getUserDefaultSettings("222");
        });
    }

    private HttpClientErrorException createErrorResponse(HttpStatus status, String message) {
        HttpClientErrorException response = mock(HttpClientErrorException.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Message", message);
        when(response.getResponseHeaders()).thenReturn(headers);
        when(response.getRawStatusCode()).thenReturn(status.value());
        return response;
    }

    @Nested
    @DisplayName("getUserDefaultSettings()")
    class GetUserDefaultSettings {

        @Test
        @DisplayName("should return the User Profile defaults for the user")
        void shouldReturnUserProfileDefaultsForUser() {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
            jurisdictionDefinition.setId("TEST");
            jurisdictionDefinition.setName("Test");
            jurisdictionDefinition.setDescription("Test Jurisdiction");
            final UserDefault userDefault = new UserDefault();
            userDefault.setJurisdictionDefinitions(singletonList(jurisdictionDefinition));
            final ResponseEntity<UserDefault> responseEntity = new ResponseEntity<>(userDefault, HttpStatus.OK);
            when(restTemplate
                .exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class)))
                .thenReturn(responseEntity);

            final UserDefault result = userRepository.getUserDefaultSettings("ccd+test@hmcts.net");
            assertThat(result, is(userDefault));
            verify(restTemplate).exchange(
                isA(URI.class), same(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class));
        }

        @Test
        @DisplayName("should throw a BadRequestException if the User Profile defaults cannot be retrieved")
        void shouldThrowExceptionIfUserProfileCannotBeRetrieved() {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            final HttpHeaders headers = new HttpHeaders();
            headers.add("Message", "User Profile data could not be retrieved");
            final RestClientResponseException exception =
                new RestClientResponseException("Error on GET", 400, "Bad Request", headers, null, null);
            when(restTemplate
                .exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class)))
                .thenThrow(exception);

            final BadRequestException badRequestException =
                assertThrows(BadRequestException.class,
                    () -> userRepository.getUserDefaultSettings("ccd+test@hmcts.net"),
                    "Expected getUserDefaultSettings() to throw, but it didn't");
            assertThat(badRequestException.getMessage(), is(headers.getFirst("Message")));
        }

        @Test
        @DisplayName("should throw a ServiceException if an error occurs retrieving the User Profile defaults")
        void shouldThrowExceptionIfErrorOnRetrievingUserProfile() {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            final RestClientResponseException exception =
                new RestClientResponseException(null, 500, "Internal Server Error", null, null, null);
            when(restTemplate
                .exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class)))
                .thenThrow(exception);

            final String userId = "ccd+test@hmcts.net";
            final ServiceException serviceException =
                assertThrows(ServiceException.class,
                    () -> userRepository.getUserDefaultSettings(userId),
                    "Expected getUserDefaultSettings() to throw, but it didn't");
            assertThat(serviceException.getMessage(), is("Problem getting user default settings for " + userId));
        }

        @Test
        @DisplayName("should throw a ServiceException if an IO error occurs retrieving the User Profile defaults")
        void shouldThrowExceptionIfIOErrorOnRetrievingUserProfile() {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            final ResourceAccessException exception =
                new ResourceAccessException("I/O Error");
            when(restTemplate
                .exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class)))
                .thenThrow(exception);

            final String userId = "ccd+test@hmcts.net";
            final ServiceException serviceException =
                assertThrows(ServiceException.class,
                    () -> userRepository.getUserDefaultSettings(userId),
                    "Expected getUserDefaultSettings() to throw, but it didn't");
            assertThat(serviceException.getMessage(), is("Problem getting user default settings for " + userId));
        }

        @Test
        @DisplayName("should make the User Profile API call with the userId converted to lowercase, prior to encoding")
        void shouldCallUserProfileWithLowercaseEncodedUserId() {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            final ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
            doReturn(responseEntity)
                .when(restTemplate)
                .exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), (Class<?>)any(Class.class));

            final String userId = "CCD+Test@HMCTS.net";
            userRepository.getUserDefaultSettings(userId);
            verify(restTemplate).exchange(
                uriCaptor.capture(), same(HttpMethod.GET), isA(HttpEntity.class), (Class<?>)any(Class.class));
            final String lowercaseEncodedUserId = "ccd%2Btest%40hmcts.net";
            assertThat(uriCaptor.getValue().getRawQuery(), containsString(lowercaseEncodedUserId));
        }
    }

    @Nested
    @DisplayName("getHighestUserClassification()")
    class GetHighestUserClassification {

        @Test
        @DisplayName("should return highest security classification for user")
        void shouldReturnHighestClassification() {
            asCaseworker();
            when(applicationParams.getCcdAccessControlCitizenRoles())
                .thenReturn(Arrays.asList("citizen", "letter-holder"));
            UserRole userRole1 = new UserRole();
            userRole1.setSecurityClassification(SecurityClassification.PRIVATE.name());
            UserRole userRole2 = new UserRole();
            userRole2.setSecurityClassification(SecurityClassification.PUBLIC.name());
            UserRole userRole3 = new UserRole();
            userRole3.setSecurityClassification(SecurityClassification.RESTRICTED.name());
            when(caseDefinitionRepository.getClassificationsForUserRoleList(anyList()))
                .thenReturn(asList(userRole1, userRole2, userRole3));

            SecurityClassification result = userRepository.getHighestUserClassification(JURISDICTION_ID);

            assertThat(result, is(SecurityClassification.RESTRICTED));
        }

        @Test
        @DisplayName("should throw exception when no user roles returned")
        void shouldThrowExceptionWhenNoUserRolesReturned() {
            asCaseworker();
            when(applicationParams.getCcdAccessControlCitizenRoles()).thenReturn(emptyList());
            when(caseDefinitionRepository.getClassificationsForUserRoleList(anyList())).thenReturn(emptyList());

            assertThrows(ServiceException.class, () -> userRepository.getHighestUserClassification(JURISDICTION_ID));
        }
    }

    @Nested
    @DisplayName("getUser()")
    class GetUser {
        @Test
        @DisplayName("should retrieve user from IDAM")
        void shouldRetrieveUserFromIdam() {
            String userId = "userId";

            IdamUser result = userRepository.getUser();

            assertThat(result.getId(), is(userId));
        }
    }

    @Nested
    @DisplayName("getCaseworkerUserRolesJurisdictions()")
    class GetCaseworkerUserRolesJurisdictions {

        @Test
        @DisplayName("test empty list of jurisdictions")
        void shouldRetrieveNoJurisdictionsWhenNotPresent() {
            List<String> roles = newArrayList(
                "caseworker", "citizen");

            mockUserInfo("userId", roles);

            final List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();

            assertAll(
                () -> assertThat(jurisdictions, hasSize(0))
            );
        }

        @Test
        @DisplayName("It should retrieve caseworkers jurisdictions")
        void shouldRetrieveCaseworkersJurisdictions() {
            List<String> roles = newArrayList(
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest1-solicitor",
                "caseworker-autotest1-private",
                "caseworker-autotest1-senior",
                "caseworker-autotest2",
                "caseworker-autotest2-solicitor",
                "caseworker-autotest2-private",
                "caseworker-autotest2-senior");

            mockUserInfo("userId", roles);

            final List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();

            assertAll(
                () -> assertThat(jurisdictions, hasSize(2)),
                () -> assertThat(jurisdictions, hasItems("autotest1", "autotest2"))
            );
        }

        @Test
        @DisplayName("It should retrieve all roles jurisdictions")
        void shouldRetrieveAllRolesJurisdictions() {
            List<String> roles = newArrayList(
                "caseworker",
                "citizen",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "otherRole-autotest1",
                "otherRole-autotest2",
                ROLE_CASEWORKER_CAA);

            mockUserInfo("userId", roles);

            final List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();

            assertAll(
                () -> assertThat(jurisdictions, hasSize(2)),
                () -> assertThat(jurisdictions, hasItems("autotest1", "autotest2"))
            );
        }

        @Test
        @DisplayName("It should ignore roles defined as cross-jurisdiction")
        void shouldIgnoreCrossJurisdictionRoles() {
            List<String> roles = singletonList(ROLE_CASEWORKER_CAA);

            mockUserInfo("userId", roles);

            final List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();

            assertAll(
                () -> assertThat(jurisdictions, hasSize(0))
            );
        }

        @Test
        @DisplayName("It should ignore non-specific roles")
        void shouldIgnoreNonSpecificRoles() {
            List<String> roles = newArrayList("pui-case-manager", "citizen", "caseworker");

            mockUserInfo("userId", roles);

            final List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();

            assertAll(
                () -> assertThat(jurisdictions, hasSize(0))
            );
        }
    }

    private void asCitizen() {
        doReturn(newAuthorities(CITIZEN, PROBATE_PRIVATE_BETA)).when(authentication)
                                                               .getAuthorities();
    }

    private void asLetterHolderCitizen() {
        doReturn(newAuthorities(LETTER_HOLDER, PROBATE_PRIVATE_BETA)).when(authentication)
                                                                     .getAuthorities();
    }

    private void asCaseworker() {
        doReturn(newAuthorities(ROLE_CASEWORKER, ROLE_CASEWORKER_TEST, ROLE_CASEWORKER_CMC)).when(authentication)
                                                                                            .getAuthorities();
    }

    private void asCaseworkerCaa() {
        doReturn(newAuthorities(ROLE_CASEWORKER_CAA)).when(authentication)
            .getAuthorities();
    }

    private void mockUserInfo(String userId) {
        mockUserInfo(userId, emptyList());
    }

    private void mockUserInfo(String userId, List<String> roles) {
        UserInfo userInfo = UserInfo.builder()
            .uid(userId)
            .roles(roles)
            .build();
        when(securityUtils.getUserInfo()).thenReturn(userInfo);
    }

    private void asOtherRoles() {
        doReturn(newAuthorities("role1", "role2")).when(authentication)
                                                  .getAuthorities();
    }

    private void initSecurityContext() {
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }

    private Collection<GrantedAuthority> newAuthorities(String... authorities) {
        return Arrays.stream(authorities)
                     .map(this::newAuthority)
                     .collect(Collectors.toSet());
    }
}
