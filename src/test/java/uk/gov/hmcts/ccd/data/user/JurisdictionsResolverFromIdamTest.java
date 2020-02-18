package uk.gov.hmcts.ccd.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

public class JurisdictionsResolverFromIdamTest {

    private final JurisdictionsResolver jurisdictionsResolver = new JurisdictionsResolverFromIdam();


    @BeforeEach
    void setUp() {
    }

    @Nested
    @DisplayName("JurisdictionsResolverFromIdam")
    class GetUserId {

        @Test
        @DisplayName("It should retrieve one user jurisdiction from idam.")
        void shouldRetrieveOneUserJurisdictionIdam() {

            final String[] roles = new String[] { "caseworker", "caseworker-autotest1",
                "caseworker-autotest1-solicitor", "caseworker-autotest1-private", "caseworker-autotest1-senior" };

            final List<String> result = jurisdictionsResolver.getJurisdictionsFromIdam(roles);

            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result.get(0), is("autotest1"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions from idam.")
        void shouldRetrieveAllUserJurisdictionIdam() {

            final String[] roles = new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest1-solicitor",
                "caseworker-autotest1-private",
                "caseworker-autotest1-senior",
                "caseworker-autotest2",
                "caseworker-autotest2-solicitor",
                "caseworker-autotest2-private",
                "caseworker-autotest2-senior"
            };

            final List<String> result = jurisdictionsResolver.getJurisdictionsFromIdam(roles);

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2"))
            );
        }

        @Test
        @DisplayName("It should retrieve all user jurisdictions from idam with no visibilities settings.")
        void shouldRetrieveAllUserJurisdictionIdamWithNoVisibilities() {

            final String[] roles = new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "caseworker-autotest3"
            };

            final List<String> result = jurisdictionsResolver.getJurisdictionsFromIdam(roles);

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

            final String[] roles = new String[] {
                "caseworker",
                "caseworker-autotest1",
                "caseworker-autotest2",
                "otherRole",
                "otherRole-autotest1",
                "otherRole-autotest2"
            };

            final List<String> result = jurisdictionsResolver.getJurisdictionsFromIdam(roles);

            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is("autotest1")),
                () -> assertThat(result.get(1), is("autotest2"))
            );
        }
    }
}
