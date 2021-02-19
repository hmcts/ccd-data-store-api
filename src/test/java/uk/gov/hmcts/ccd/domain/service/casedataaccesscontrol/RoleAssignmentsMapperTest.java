package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RoleAssignmentMapperTest")
class RoleAssignmentsMapperTest {
    public static final String USER_ID = "user1";
    public static final String CASE_ID1 = "caseId1";
    public static final String CASE_ID2 = "caseId2";
    public static final String ASSIGNMENT_1 = "assignment1";
    public static final String ASSIGNMENT_2 = "assignment2";
    private static final LocalDateTime BEGIN_TIME = LocalDateTime.of(2015, 10, 21, 13, 32);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2215, 11, 22, 14, 33);
    private static final LocalDateTime CREATED = LocalDateTime.of(2020, 12, 23, 15, 34);

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

            RoleAssignments mapped = RoleAssignmentsMapper.INSTANCE.toRoleAssignments(response);

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
                                 is(roleAssignment2.getAttributes().getCaseId()))
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
            .grantType("STANDARD") // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
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
            .jurisdiction("DIVORCE")
            .caseId(caseId)
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED") // SALARIED, FEEPAY
            .build();
    }
}
