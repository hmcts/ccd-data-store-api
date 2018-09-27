package uk.gov.hmcts.ccd.data.user;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
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
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(asList(ROLE_CASEWORKER_CMC)),
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
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(asList(CITIZEN)),
                    () -> verifyNoMoreInteractions(caseDefinitionRepository)
                );
            }

            @Test
            @DisplayName("should consider `letter-holder` role")
            void shouldConsiderLetterHolder() {
                asLetterHolderCitizen();

                userRepository.getUserClassifications(JURISDICTION_ID);

                assertAll(
                    () -> verify(caseDefinitionRepository).getClassificationsForUserRoleList(asList(LETTER_HOLDER)),
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
            when(caseDefinitionRepository.getClassificationsForUserRoleList(anyListOf(String.class))).thenReturn(asList(userRole1, userRole2, userRole3));

            SecurityClassification result = userRepository.getHighestUserClassification();

            assertThat(result, is(SecurityClassification.RESTRICTED));
        }

        @Test
        @DisplayName("should throw exception when no user roles returned")
        void shouldThrowExceptionWhenNoUserRolesReturned() {
            asCaseworker();
            when(caseDefinitionRepository.getClassificationsForUserRoleList(anyListOf(String.class))).thenReturn(emptyList());

            assertThrows(ServiceException.class, () -> userRepository.getHighestUserClassification());
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
