package uk.gov.hmcts.ccd.data.user;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.reform.auth.checker.spring.serviceanduser.ServiceAndUserDetails;

class DefaultUserRepositoryTest {

    private static final String ROLE_CASEWORKER = "caseworker";
    private static final String ROLE_CASEWORKER_TEST = "caseworker-test";
    private static final String ROLE_CASEWORKER_CMC = "caseworker-cmc";
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
    private SecurityContext securityContext;

    @Mock
    private ServiceAndUserDetails principal;

    @InjectMocks
    private DefaultUserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        initSecurityContext();
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
        @DisplayName("when citizen")
        class WhenCitizen {

            @Test
            @DisplayName("should consider `citizen` role")
            void shouldConsiderCitizen() {
                asCitizen();

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
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class))).thenThrow(response);
            doThrow(response).when(restTemplate).exchange(anyString(), any(), any(), any(Class.class), anyMap());

            userRepository.getUserDefaultSettings("222");
        });
    }

    @Test
    void getUserDefaultSettingsShouldReturnResourceNotFoundExceptionWhen404() {
        assertThrows(ResourceNotFoundException.class, () -> {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            HttpClientErrorException response = createErrorResponse(HttpStatus.NOT_FOUND, "some message");
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class))).thenThrow(response);

            userRepository.getUserDefaultSettings("222");
        });
    }

    @Test
    void getUserDefaultSettingsShouldReturnBadRequestWhenNot404() {
        assertThrows(BadRequestException.class, () -> {
            when(applicationParams.userDefaultSettingsURL()).thenReturn("http://test.hmcts.net/users?uid={uid}");
            HttpClientErrorException response = createErrorResponse(HttpStatus.BAD_GATEWAY, "some message");
            when(restTemplate.exchange(isA(URI.class), eq(HttpMethod.GET), isA(HttpEntity.class), eq(UserDefault.class))).thenThrow(response);

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
            final Jurisdiction jurisdiction = new Jurisdiction();
            jurisdiction.setId("TEST");
            jurisdiction.setName("Test");
            jurisdiction.setDescription("Test Jurisdiction");
            final UserDefault userDefault = new UserDefault();
            userDefault.setJurisdictions(singletonList(jurisdiction));
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
            when(caseDefinitionRepository.getClassificationsForUserRoleList(anyList())).thenReturn(emptyList());

            assertThrows(ServiceException.class, () -> userRepository.getHighestUserClassification(JURISDICTION_ID));
        }
    }

    @Nested
    @DisplayName("getUser()")
    class GetUser {
        private static final String URL = "url";

        @BeforeEach
        void setUp() {
            when(applicationParams.idamUserProfileURL()).thenReturn(URL);
        }

        @Test
        @DisplayName("should retrieve user from IDAM")
        void shouldRetrieveUserFromIdam() {
            IdamUser idamUser = new IdamUser();
            when(restTemplate.exchange(eq(URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(IdamUser.class)))
                .thenReturn(ResponseEntity.ok(idamUser));

            IdamUser result = userRepository.getUser();

            assertThat(result, is(idamUser));
            verify(applicationParams).idamUserProfileURL();
            verify(securityUtils).userAuthorizationHeaders();
            verify(applicationParams).idamUserProfileURL();
            verify(restTemplate).exchange(eq(URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(IdamUser.class));
        }

        @Test
        @DisplayName("should throw exception when rest call fails")
        void shouldThrowExceptionWhenRestCallFails() {
            when(applicationParams.idamUserProfileURL()).thenReturn("url");
            doThrow(new RestClientException("Error")).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(IdamUser.class));

            assertThrows(ServiceException.class, () -> userRepository.getUser());
        }
    }

    private void asCitizen() {
        doReturn(newAuthorities(CITIZEN, PROBATE_PRIVATE_BETA)).when(principal)
                                                               .getAuthorities();
    }

    private void asLetterHolderCitizen() {
        doReturn(newAuthorities(LETTER_HOLDER, PROBATE_PRIVATE_BETA)).when(principal)
                                                                     .getAuthorities();
    }

    private void asCaseworker() {
        doReturn(newAuthorities(ROLE_CASEWORKER, ROLE_CASEWORKER_TEST, ROLE_CASEWORKER_CMC)).when(principal)
                                                                                            .getAuthorities();
    }

    private void asOtherRoles() {
        doReturn(newAuthorities("role1", "role2")).when(principal)
                                                  .getAuthorities();
    }

    private void initSecurityContext() {
        doReturn(principal).when(authentication).getPrincipal();
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
