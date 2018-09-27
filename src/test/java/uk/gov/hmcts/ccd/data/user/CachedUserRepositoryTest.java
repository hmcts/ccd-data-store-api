package uk.gov.hmcts.ccd.data.user;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;

class CachedUserRepositoryTest {

    private static final String JURISDICTION_ID = "DIVORCE";
    @Mock
    private UserRepository userRepository;
    private CachedUserRepository cachedUserRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        cachedUserRepository = new CachedUserRepository(userRepository);
    }

    @Nested
    @DisplayName("getUserDetails()")
    class getUserDetails {

        @Test
        @DisplayName("should initially retrieve user details from decorated repository")
        void shouldRetrieveUserDetailsFromDecorated () {
            final IDAMProperties expectedUserDetails = new IDAMProperties();
            doReturn(expectedUserDetails).when(userRepository).getUserDetails();

            final IDAMProperties userDetails = cachedUserRepository.getUserDetails();

            assertAll(
                () -> assertThat(userDetails, is(expectedUserDetails)),
                () -> verify(userRepository, times(1)).getUserDetails()
            );
        }

        @Test
        @DisplayName("should cache user details for subsequent calls")
        void shouldCacheUserDetailsForSubsequentCalls () {
            final IDAMProperties expectedUserDetails = new IDAMProperties();
            doReturn(expectedUserDetails).when(userRepository).getUserDetails();

            cachedUserRepository.getUserDetails();

            verify(userRepository, times(1)).getUserDetails();

            doReturn(new IDAMProperties()).when(userRepository).getUserDetails();

            final IDAMProperties userDetails = cachedUserRepository.getUserDetails();

            assertAll(
                () -> assertThat(userDetails, is(expectedUserDetails)),
                () -> verifyNoMoreInteractions(userRepository)
            );
        }
    }

    @Nested
    @DisplayName("getUserRoles()")
    class getUserRoles {

        @Test
        @DisplayName("should initially retrieve user roles from decorated repository")
        void shouldRetrieveUserRolesFromDecorated () {
            final HashSet<String> expectedUserRoles = Sets.newHashSet("role1", "role2");
            doReturn(expectedUserRoles).when(userRepository).getUserRoles();

            final Set<String> userRoles = cachedUserRepository.getUserRoles();

            assertAll(
                () -> assertThat(userRoles, is(expectedUserRoles)),
                () -> verify(userRepository, times(1)).getUserRoles()
            );
        }

        @Test
        @DisplayName("should cache user roles for subsequent calls")
        void shouldCacheUserRolesForSubsequentCalls () {
            final HashSet<String> expectedUserRoles = Sets.newHashSet("role1", "role2");
            doReturn(expectedUserRoles).when(userRepository).getUserRoles();

            cachedUserRepository.getUserRoles();

            verify(userRepository, times(1)).getUserRoles();

            doReturn(Sets.newHashSet("role3", "role4")).when(userRepository).getUserRoles();

            final Set<String> userRoles = cachedUserRepository.getUserRoles();

            assertAll(
                () -> assertThat(userRoles, is(expectedUserRoles)),
                () -> verifyNoMoreInteractions(userRepository)
            );
        }
    }


    @Nested
    @DisplayName("getUserClassifications()")
    class getUserClassifications {

        @Test
        @DisplayName("should initially retrieve classifications from decorated repository")
        void shouldRetrieveClassificationsFromDecorated () {
            final HashSet<SecurityClassification> expectedClassifications = Sets.newHashSet(PUBLIC, PRIVATE);
            doReturn(expectedClassifications).when(userRepository).getUserClassifications(JURISDICTION_ID);

            final Set<SecurityClassification> classifications = cachedUserRepository.getUserClassifications(
                JURISDICTION_ID);

            assertAll(
                () -> assertThat(classifications, is(expectedClassifications)),
                () -> verify(userRepository, times(1)).getUserClassifications(JURISDICTION_ID)
            );
        }

        @Test
        @DisplayName("should cache classifications for subsequent calls")
        void shouldCacheClassificationForSubsequentCalls () {
            final HashSet<SecurityClassification> expectedClassifications = Sets.newHashSet(PUBLIC);
            doReturn(expectedClassifications).when(userRepository).getUserClassifications(JURISDICTION_ID);

            cachedUserRepository.getUserClassifications(JURISDICTION_ID);

            verify(userRepository, times(1)).getUserClassifications(JURISDICTION_ID);

            doReturn(Sets.newHashSet(PRIVATE, RESTRICTED)).when(userRepository).getUserClassifications(JURISDICTION_ID);

            final Set<SecurityClassification> classifications = cachedUserRepository.getUserClassifications(
                JURISDICTION_ID);

            assertAll(
                () -> assertThat(classifications, is(expectedClassifications)),
                () -> verifyNoMoreInteractions(userRepository)
            );
        }
    }

    @Nested
    @DisplayName("getHighestUserClassification()")
    class GetHighestUserClassification {
        @Test
        @DisplayName("should initially retrieve highest security classification from repository and from cache for subsequent calls")
        void shouldRetrieveUserRolesFromDecorated() {
            when(userRepository.getHighestUserClassification()).thenReturn(PRIVATE);

            SecurityClassification classification1 = cachedUserRepository.getHighestUserClassification();

            assertAll(
                () -> assertThat(classification1, is(PRIVATE)),
                () -> verify(userRepository, times(1)).getHighestUserClassification()
            );

            SecurityClassification classification2 = cachedUserRepository.getHighestUserClassification();

            assertAll(
                () -> assertThat(classification2, is(PRIVATE)),
                () -> verifyNoMoreInteractions(userRepository)
            );
        }
    }
}
