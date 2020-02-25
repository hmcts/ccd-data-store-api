package uk.gov.hmcts.ccd.data.user;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class IDAMJurisdictionsResolverTest {

    @Mock
    private UserRepository userRepoMock;

    @InjectMocks
    private IDAMJurisdictionsResolver jurisdictionsResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("JurisdictionsResolverFromIdam")
    class GetUserId {

        @Test
        @DisplayName("It should retrieve one user jurisdiction from idam.")
        void shouldRetrieveOneUserJurisdictionIdam() {

            mockIDAMProperties(new String[] { "caseworker", "caseworker-autotest1",
                "caseworker-autotest1-solicitor", "caseworker-autotest1-private", "caseworker-autotest1-senior" });

            final List<String> result = jurisdictionsResolver.getJurisdictions();

            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result.get(0), is("autotest1"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions from idam.")
        void shouldRetrieveAllUserJurisdictionIdam() {

            mockIDAMProperties(new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest1-solicitor",
                "caseworker-autotest1-private",
                "caseworker-autotest1-senior",
                "caseworker-autotest2",
                "caseworker-autotest2-solicitor",
                "caseworker-autotest2-private",
                "caseworker-autotest2-senior"
            });

            final List<String> result = jurisdictionsResolver.getJurisdictions();

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions from idam with no visibilities settings.")
        void shouldRetrieveAllUserJurisdictionIdamWithNoVisibilities() {

            mockIDAMProperties(new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "caseworker-autotest3"
            });

            final List<String> result = jurisdictionsResolver.getJurisdictions();

            assertAll(
                () -> assertThat(result.size(), is(3)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2")),
                () -> assertThat(result.get(2), is("autotest3"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions for a user with many roles.")
        void shouldRetrieveAllUserJurisdictionIdamUserWithManyRoles() {

            mockIDAMProperties(new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "otherRole",
                "otherRole-autotest1",
                "otherRole-autotest2"
            });

            final List<String> result = jurisdictionsResolver.getJurisdictions();

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions for a user with citizen role.")
        void shouldRetrieveAllUserJurisdictionIDAMUserWithCitizenRole() {

            mockIDAMProperties(new String[] {
                "caseworker",
                "citizen",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "otherRole-autotest1",
                "otherRole-autotest2"
            });

            final List<String> result = jurisdictionsResolver.getJurisdictions();

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2"))
            );
        }
    }

    private void mockIDAMProperties(String[] roles) {
        IDAMProperties idamProperties = mock(IDAMProperties.class);
        doReturn(roles).when(idamProperties).getRoles();
        doReturn(idamProperties).when(userRepoMock).getUserDetails();
    }
}
