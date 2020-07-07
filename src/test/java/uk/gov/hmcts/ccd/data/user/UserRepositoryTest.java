package uk.gov.hmcts.ccd.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.AuthCheckerConfiguration;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UserRoleBuilder.aUserRole;

public class UserRepositoryTest {

    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA2 = "caseworker-probate-loa2";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

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

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        userRepository = spy(new DefaultUserRepository(applicationParams,
                                                       caseDefinitionRepository, securityUtils, restTemplate, authCheckerConfiguration));
    }

    @Nested
    @DisplayName("getUserRoles()")
    class GetUserRoles {

        @Test
        @DisplayName("should retrieve roles from security principals")
        void shouldRetrieveRolesFromPrincipal() {
            MockUtils.setSecurityAuthorities(authentication, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA2, CASEWORKER_DIVORCE);

            Set<String> userRoles = userRepository.getUserRoles();

            verify(securityContext, times(1)).getAuthentication();
            verify(authentication, times(1)).getAuthorities();
            assertThat(userRoles, hasItems(CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA2, CASEWORKER_DIVORCE));
        }

        @Test
        @DisplayName("should retrieve no role if no relevant role found")
        void shouldRetrieveNoRoleIfNoRelevantRoleFound() {
            MockUtils.setSecurityAuthorities(authentication);

            Set<String> userRoles = userRepository.getUserRoles();

            assertThat(userRoles, is(emptyCollectionOf(String.class)));
        }

    }

    @Nested
    @DisplayName("getUserClassifications()")
    class GetUserClassifications {

        @Test
        @DisplayName("should retrieve roles from user repository")
        public void shouldRetrieveRolesFromPrincipal() {
            doReturn(newHashSet(CASEWORKER_PROBATE_LOA1)).when(userRepository).getUserRoles();

            userRepository.getUserClassifications("PROBATE");

            verify(userRepository, times(1)).getUserRoles();
        }

        @Test
        @DisplayName("should list classifications for roles")
        public void shouldRetrieveClassificationsForRoles() {
            final List<UserRole> userRoleList = Arrays.asList(
                aUserRole().withRole(CASEWORKER_PROBATE_LOA1).withSecurityClassification(PUBLIC).build(),
                aUserRole().withRole(CASEWORKER_PROBATE_LOA2).withSecurityClassification(PRIVATE).build()
            );
            List<String> roles = Arrays.asList(CASEWORKER_PROBATE_LOA2, CASEWORKER_PROBATE_LOA1);
            when(caseDefinitionRepository.getClassificationsForUserRoleList(roles)).thenReturn(userRoleList);
            doReturn(newHashSet(CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA2)).when(userRepository).getUserRoles();

            final Set<SecurityClassification> userClassifications = userRepository.getUserClassifications("PROBATE");

            assertThat(userClassifications, hasSize(2));
            assertThat(userClassifications, hasItems(PUBLIC, PRIVATE));
        }

        @Test
        @DisplayName("should filter out roles irrelevant to the current jurisdiction")
        public void shouldFilterOutRolesForOtherJurisdiction() {
            doReturn(newHashSet(CASEWORKER_DIVORCE)).when(userRepository).getUserRoles();

            assertNoClassifications();
        }

        @Test
        @DisplayName("should not return security classification if null role returned from def store")
        public void shouldFailToGetUserClassificationIfNoRelevantRoleFound() {
            doReturn(newHashSet(CASEWORKER_PROBATE_LOA1)).when(userRepository).getUserRoles();

            assertNoClassifications();
        }

        @Test
        @DisplayName("should retrieve no security classification if empty list of roles returned from case definition store")
        public void shouldRetrieveNoSecurityClassificationIfEmptyListOfRolesFromCaseDefinition() {
            doReturn(newHashSet()).when(userRepository).getUserRoles();

            assertNoClassifications();
        }

        @Test
        @DisplayName("should fail to retrieve security classifications if unable to talk to definition store")
        public void shouldFailIfExceptionWhileRetrievingUserRoleFromCaseDefinition() {
            doReturn(newHashSet(CASEWORKER_PROBATE_LOA1)).when(userRepository).getUserRoles();
            doThrow(ServiceException.class).when(caseDefinitionRepository).getClassificationsForUserRoleList(Arrays.asList(CASEWORKER_PROBATE_LOA1));

            assertThrows(ServiceException.class, () -> userRepository.getUserClassifications("PROBATE"),
                         "Classification retrieval should have failed");
        }

        private void assertNoClassifications() {
            final Set<SecurityClassification> userClassifications = userRepository.getUserClassifications("PROBATE");

            assertThat(userClassifications, hasSize(0));
        }
    }
}
