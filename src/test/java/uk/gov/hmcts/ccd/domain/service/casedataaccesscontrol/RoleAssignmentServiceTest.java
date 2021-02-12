package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRecord;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRecordAttribute;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.definition.RoleAssignment;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RoleAssignmentService")
class RoleAssignmentServiceTest {

    public static final String USER_ID = "user1";
    public static final String CASE_ID1 = "caseId1";
    public static final String CASE_ID2 = "caseId2";
    public static final String ASSIGNMENT_1 = "assignment1";
    public static final String ASSIGNMENT_2 = "assignment2";

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository);
    }

    @Nested
    @DisplayName("getRoleAssignments()")
    class GetRoleAssignments {

        @Test
        public void shouldGetRoleAssignments() {
            RoleAssignmentRecord roleAssignmentRecord1 = createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID1);
            RoleAssignmentRecord roleAssignmentRecord2 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2);
            RoleAssignmentResponse response = createRoleAssignmentResponse(asList(
                roleAssignmentRecord1, roleAssignmentRecord2
            ));
            BDDMockito.given(roleAssignmentRepository.getRoleAssignments(USER_ID)).willReturn(response);

            List<RoleAssignment> roleAssignments = roleAssignmentService.getRoleAssignments(USER_ID);

            assertAll(
                () -> assertThat(roleAssignments.size(), is(2)),

                () -> assertThat(roleAssignments.get(0).getId(), is(ASSIGNMENT_1)),
                () -> assertThat(roleAssignments.get(0).getActorIdType(), is(roleAssignmentRecord1.getActorIdType())),
                () -> assertThat(roleAssignments.get(0).getActorId(), is(roleAssignmentRecord1.getActorId())),
                () -> assertThat(roleAssignments.get(0).getRoleType(), is(roleAssignmentRecord1.getRoleType())),
                () -> assertThat(roleAssignments.get(0).getRoleName(), is(roleAssignmentRecord1.getRoleName())),
                () -> assertThat(roleAssignments.get(0).getClassification(),
                                 is(roleAssignmentRecord1.getClassification())),
                () -> assertThat(roleAssignments.get(0).getGrantType(), is(roleAssignmentRecord1.getGrantType())),
                () -> assertThat(roleAssignments.get(0).getRoleCategory(), is(roleAssignmentRecord1.getRoleCategory())),
                () -> assertThat(roleAssignments.get(0).getReadOnly(), is(roleAssignmentRecord1.getReadOnly())),
                () -> assertThat(roleAssignments.get(0).getBeginTime(), is(roleAssignmentRecord1.getBeginTime())),
                () -> assertThat(roleAssignments.get(0).getEndTime(), is(roleAssignmentRecord1.getEndTime())),
                () -> assertThat(roleAssignments.get(0).getCreated(), is(roleAssignmentRecord1.getCreated())),
                () -> assertThat(roleAssignments.get(0).getAuthorisations().size(), is(0)),

                () -> assertThat(roleAssignments.get(0).getAttributes().get(0).getJurisdiction(),
                                 is(roleAssignmentRecord1.getAttributes().get(0).getJurisdiction())),
                () -> assertThat(roleAssignments.get(0).getAttributes().get(0).getCaseId(),
                                 is(roleAssignmentRecord1.getAttributes().get(0).getCaseId())),
                () -> assertThat(roleAssignments.get(0).getAttributes().get(0).getRegion(),
                                 is(roleAssignmentRecord1.getAttributes().get(0).getRegion())),
                () -> assertThat(roleAssignments.get(0).getAttributes().get(0).getLocation(),
                                 is(roleAssignmentRecord1.getAttributes().get(0).getLocation())),
                () -> assertThat(roleAssignments.get(0).getAttributes().get(0).getContractType(),
                                 is(roleAssignmentRecord1.getAttributes().get(0).getContractType())),

                () -> assertThat(roleAssignments.get(1).getId(), is(ASSIGNMENT_2)),
                () -> assertThat(roleAssignments.get(1).getAttributes().get(0).getCaseId(),
                                 is(roleAssignmentRecord2.getAttributes().get(0).getCaseId()))
            );
        }
    }

    private static RoleAssignmentResponse createRoleAssignmentResponse(
        List<RoleAssignmentRecord> roleAssignmentRecords) {
        return RoleAssignmentResponse.builder()
            .roleAssignmentRecords(roleAssignmentRecords)
            .build();
    }

    private static RoleAssignmentRecord createRoleAssignmentRecord(String id, String caseId) {
        return RoleAssignmentRecord.builder()
        .id(id)
        .actorIdType("IDAM") // currently IDAM
        .actorId("aecfec12-1f9a-40cb-bd8c-7a9f3506e67c")
        .roleType("CASE") // ORGANISATION, CASE
        .roleName("judiciary")
        .classification("PUBLIC")
        .grantType("STANDARD") // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
        .roleCategory("JUDICIAL") // JUDICIAL, STAFF
        .readOnly(false)
        .beginTime("2010-01-2423:13:01Z") //  "YYYY-MM-DDTHH:MI:SSZ"
        .endTime("2050-01-2423:13:01Z")
        .created("2010-01-2420:13:01Z")
        .authorisations(Collections.emptyList())
        .attributes(singletonList(createRoleAssignmentRecordAttribute(caseId)))
            .build();
    }

    private static RoleAssignmentRecordAttribute createRoleAssignmentRecordAttribute(String caseId) {
        return RoleAssignmentRecordAttribute.builder()
            .jurisdiction("DIVORCE")
            .caseId(caseId)
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED") // SALARIED, FEEPAY
            .build();
    }
}
