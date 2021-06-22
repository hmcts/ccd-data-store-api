package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("RoleAssignmentMapperTest")
class RoleAssignmentsMapperTest {
    public static final String USER_ID = "user1";
    public static final String CASE_ID1 = "caseId1";
    public static final String CASE_ID2 = "caseId2";
    public static final String ASSIGNMENT_1 = "assignment1";
    public static final String ASSIGNMENT_2 = "assignment2";
    private static final Instant BEGIN_TIME = Instant.parse("2015-10-21T13:32:21.123Z");
    private static final Instant END_TIME = Instant.parse("2215-11-04T14:43:22.456Z");
    private static final Instant CREATED = Instant.parse("2020-12-04T15:54:23.789Z");

    private final RoleAssignmentsMapper instance = new RoleAssignmentsMapperImpl();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

    }

    @Nested
    @DisplayName("toRoleAssignments()")
    class ToRoleAssignments {

        @Test
        public void shouldMapToRoleAssignments() {
            RoleAssignmentResource roleAssignment1 = createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID1);
            RoleAssignmentResource roleAssignment2 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2);
            RoleAssignmentResponse response = createRoleAssignmentResponse(asList(
                roleAssignment1, roleAssignment2
            ));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();

            assertAll(
                () -> assertThat(roleAssignments.size(), is(2)),

                () -> assertThat(roleAssignments.get(0).getId(), is(ASSIGNMENT_1)),
                () -> assertThat(roleAssignments.get(0).getActorIdType(), is(roleAssignment1.getActorIdType())),
                () -> assertThat(roleAssignments.get(0).getActorId(), is(roleAssignment1.getActorId())),
                () -> assertThat(roleAssignments.get(0).getRoleType(), is(roleAssignment1.getRoleType())),
                () -> assertThat(roleAssignments.get(0).getRoleName(), is(roleAssignment1.getRoleName())),
                () -> assertThat(roleAssignments.get(0).getClassification(),
                                 is(roleAssignment1.getClassification())),
                () -> assertThat(roleAssignments.get(0).getGrantType(), is(roleAssignment1.getGrantType())),
                () -> assertThat(roleAssignments.get(0).getRoleCategory(), is(roleAssignment1.getRoleCategory())),
                () -> assertThat(roleAssignments.get(0).getReadOnly(), is(roleAssignment1.getReadOnly())),
                () -> assertThat(roleAssignments.get(0).getBeginTime(), is(roleAssignment1.getBeginTime())),
                () -> assertThat(roleAssignments.get(0).getEndTime(), is(roleAssignment1.getEndTime())),
                () -> assertThat(roleAssignments.get(0).getCreated(), is(roleAssignment1.getCreated())),
                () -> assertThat(roleAssignments.get(0).getAuthorisations().size(), is(0)),

                () -> assertThat(roleAssignments.get(0).getAttributes().getJurisdiction(),
                                 is(roleAssignment1.getAttributes().getJurisdiction())),
                () -> assertThat(roleAssignments.get(0).getAttributes().getCaseId(),
                                 is(roleAssignment1.getAttributes().getCaseId())),
                () -> assertThat(roleAssignments.get(0).getAttributes().getRegion(),
                                 is(roleAssignment1.getAttributes().getRegion())),
                () -> assertThat(roleAssignments.get(0).getAttributes().getLocation(),
                                 is(roleAssignment1.getAttributes().getLocation())),
                () -> assertThat(roleAssignments.get(0).getAttributes().getContractType(),
                                 is(roleAssignment1.getAttributes().getContractType())),

                () -> assertThat(roleAssignments.get(1).getId(), is(ASSIGNMENT_2)),
                () -> assertThat(roleAssignments.get(1).getAttributes().getCaseId(),
                                 is(roleAssignment2.getAttributes().getCaseId())),
                () -> assertThat(roleAssignments.get(1).getAttributes().getJurisdiction(),
                    is(roleAssignment2.getAttributes().getJurisdiction())),
                () -> assertThat(roleAssignments.get(1).getAttributes().getCaseType(),
                    is(roleAssignment2.getAttributes().getCaseType())),
                () -> assertThat(roleAssignments.get(1).getAttributes().getContractType(),
                    is(roleAssignment2.getAttributes().getContractType())),
                () -> assertThat(roleAssignments.get(1).getAttributes().getLocation(),
                    is(roleAssignment2.getAttributes().getLocation())),
                () -> assertThat(roleAssignments.get(1).getAttributes().getRegion(),
                    is(roleAssignment2.getAttributes().getRegion()))
            );
        }

        @Test
        public void shouldMapNullRoleAssignmentResponse() {
            assertNull(instance.toRoleAssignments(null));
        }

        @Test
        public void shouldMapNullRoleAssignmentResourceList() {
            RoleAssignmentResponse response = createRoleAssignmentResponse(null);

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignments();
            assertNull(roleAssignments);
        }

        @Test
        public void shouldMapNullRoleAssignmentResource() {
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
        public void shouldMapNullRoleAssignmentAttributes() {
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
                () -> assertNull(roleAssignments.get(0).getAttributes())
            );
        }
    }

    private static RoleAssignmentResponse createRoleAssignmentResponse(
        List<RoleAssignmentResource> roleAssignments) {
        return RoleAssignmentResponse.builder()
            .roleAssignments(roleAssignments)
            .build();
    }

    private static RoleAssignmentResource createRoleAssignmentRecord(String id, String caseId) {
        return RoleAssignmentResource.builder()
            .id(id)
            .actorIdType("IDAM") // currently IDAM
            .actorId("aecfec12-1f9a-40cb-bd8c-7a9f3506e67c")
            .roleType("CASE") // ORGANISATION, CASE
            .roleName("judiciary")
            .classification("PUBLIC")
            .grantType(GrantType.STANDARD.name()) // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
            .roleCategory("JUDICIAL") // JUDICIAL, STAFF
            .readOnly(false)
            .beginTime(BEGIN_TIME)
            .endTime(END_TIME)
            .created(CREATED)
            .authorisations(Collections.emptyList())
            .attributes(createRoleAssignmentRecordAttribute(caseId))
            .build();
    }

    private static RoleAssignmentAttributesResource createRoleAssignmentRecordAttribute(String caseId) {
        return RoleAssignmentAttributesResource.builder()
            .jurisdiction(Optional.of("DIVORCE"))
            .caseId(Optional.of(caseId))
            .caseType(Optional.of("FT_Tabs"))
            .region(Optional.of("Hampshire"))
            .location(Optional.of("Southampton"))
            .contractType(Optional.of("SALARIED")) // SALARIED, FEEPAY
            .build();
    }
}
