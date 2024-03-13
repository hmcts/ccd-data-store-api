package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRequestResponse;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentRecord;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentRequestResponse;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentResponse;

@DisplayName("RoleAssignmentMapperTest")
class RoleAssignmentsMapperTest {
    public static final String USER_ID = "user1";
    public static final String CASE_ID1 = "caseId1";
    public static final String CASE_ID2 = "caseId2";
    public static final String ASSIGNMENT_1 = "assignment1";
    public static final String ASSIGNMENT_2 = "assignment2";
    public static final String CASE_GROUP_ID1 = "caseGroupId1";

    private final RoleAssignmentsMapper instance = RoleAssignmentsMapper.INSTANCE;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("toRoleAssignments(RoleAssignmentRequestResponse)")
    class ToRoleAssignmentsFromRoleAssignmentRequestResponse {

        @Test
        void shouldMapToRoleAssignments() {
            RoleAssignmentResource roleAssignment1 = createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID1);
            RoleAssignmentResource roleAssignment2 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2);
            RoleAssignmentRequestResponse response = createRoleAssignmentRequestResponse(asList(
                roleAssignment1, roleAssignment2
            ));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();

            assertAll(
                () -> assertThat(roleAssignments.size(), is(2)),

                () -> assertThat(roleAssignments.get(0), matchesRoleAssignmentResource(roleAssignment1)),
                () -> assertThat(roleAssignments.get(1), matchesRoleAssignmentResource(roleAssignment2))
            );

        }

        @Test
        void shouldMapNullRoleAssignmentRequestResponse() {
            assertNull(instance.toRoleAssignments((RoleAssignmentRequestResponse)null));
        }

        @Test
        void shouldMapNullRoleAssignmentResponse() {
            RoleAssignmentRequestResponse response = RoleAssignmentRequestResponse.builder()
                .roleAssignmentResponse(null)
                .build();

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertNull(roleAssignments);
        }

        @Test
        void shouldMapNullRequestedRolesList() {
            RoleAssignmentRequestResponse response = createRoleAssignmentRequestResponse(null);

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertNull(roleAssignments);
        }

        @Test
        void shouldMapNullRoleAssignmentResource() {
            RoleAssignmentResource roleAssignment = null;
            RoleAssignmentRequestResponse response = createRoleAssignmentRequestResponse(singletonList(roleAssignment));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertAll(
                () -> assertThat(roleAssignments.size(), is(1)),
                () -> assertNull(roleAssignments.get(0))
            );
        }
    }

    @Nested
    @DisplayName("toRoleAssignments(RoleAssignmentResponse)")
    class ToRoleAssignmentsFromRoleAssignmentResponse {

        @Test
        void shouldMapToRoleAssignments() {
            RoleAssignmentResource roleAssignment1 = createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID1);
            RoleAssignmentResource roleAssignment2 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2);
            RoleAssignmentResource roleAssignment3 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2, CASE_GROUP_ID1);
            RoleAssignmentResponse response = createRoleAssignmentResponse(asList(
                roleAssignment1, roleAssignment2, roleAssignment3
            ));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();

            assertAll(
                () -> assertThat(roleAssignments.size(), is(3)),

                () -> assertThat(roleAssignments.get(0), matchesRoleAssignmentResource(roleAssignment1)),
                () -> assertThat(roleAssignments.get(1), matchesRoleAssignmentResource(roleAssignment2)),
                () -> assertThat(roleAssignments.get(2), matchesRoleAssignmentResource(roleAssignment3))
            );
        }

        @Test
        void shouldMapNullRoleAssignmentResponse() {
            assertNull(instance.toRoleAssignments((RoleAssignmentResponse)null));
        }

        @Test
        void shouldMapNullRoleAssignmentResourceList() {
            RoleAssignmentResponse response = createRoleAssignmentResponse(null);

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertNull(roleAssignments);
        }

        @Test
        void shouldMapNullRoleAssignmentResource() {
            RoleAssignmentResource roleAssignment = null;
            RoleAssignmentResponse response = createRoleAssignmentResponse(singletonList(roleAssignment));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertAll(
                () -> assertThat(roleAssignments.size(), is(1)),
                () -> assertNull(roleAssignments.get(0))
            );
        }

        @Test
        void shouldMapNullRoleAssignmentAttributes() {
            RoleAssignmentResource roleAssignment = RoleAssignmentResource.builder()
                .id(ASSIGNMENT_1)
                .attributes(null)
                .build();
            RoleAssignmentResponse response = createRoleAssignmentResponse(singletonList(roleAssignment));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertAll(
                () -> assertThat(roleAssignments.size(), is(1)),
                () -> assertThat(roleAssignments.get(0).getId(), is(ASSIGNMENT_1)),
                () -> assertNotNull(roleAssignments.get(0).getAttributes())
            );
        }
    }

    private <T> Matcher<T> matchesRoleAssignmentResource(RoleAssignmentResource expected) {
        return new BaseMatcher<T>() {

            @Override
            public boolean matches(Object o) {
                return o instanceof RoleAssignment
                    && ((RoleAssignment) o).getId().equals(expected.getId())
                    && ((RoleAssignment) o).getActorIdType().equals(expected.getActorIdType())
                    && ((RoleAssignment) o).getActorId().equals(expected.getActorId())
                    && ((RoleAssignment) o).getRoleType().equals(expected.getRoleType())
                    && ((RoleAssignment) o).getRoleName().equals(expected.getRoleName())
                    && ((RoleAssignment) o).getClassification().equals(expected.getClassification())
                    && ((RoleAssignment) o).getGrantType().equals(expected.getGrantType())
                    && ((RoleAssignment) o).getRoleCategory().equals(expected.getRoleCategory())
                    && ((RoleAssignment) o).getReadOnly().equals(expected.getReadOnly())
                    && ((RoleAssignment) o).getBeginTime().equals(expected.getBeginTime())
                    && ((RoleAssignment) o).getEndTime().equals(expected.getEndTime())
                    && ((RoleAssignment) o).getCreated().equals(expected.getCreated())
                    && ((RoleAssignment) o).getAuthorisations().containsAll(expected.getAuthorisations())

                    && equals(((RoleAssignment) o).getAttributes(), expected.getAttributes());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a RoleAssignment with ID " + expected.getId());
            }

            private boolean equals(RoleAssignmentAttributes actual, RoleAssignmentAttributesResource expected) {
                return actual.getJurisdiction().equals(expected.getJurisdiction())
                    && actual.getCaseType().equals(expected.getCaseType())
                    && actual.getCaseId().equals(expected.getCaseId())
                    && actual.getRegion().equals(expected.getRegion())
                    && actual.getLocation().equals(expected.getLocation())
                    && actual.getContractType().equals(expected.getContractType());
            }
        };
    }

}
