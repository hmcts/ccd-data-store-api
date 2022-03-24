package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("RoleAssignmentResourceTest")
class RoleAssignmentResourceTest {

    private static final String CASE_ID = "111111";

    @Test
    @DisplayName("shouldPassForIsAnExpiredRoleAssignment")
    void shouldPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 3600000;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        roleAssignments.getRoleAssignments().get(0).isNotExpiredRoleAssignment();
        assertThat(roleAssignments.getRoleAssignments().get(0).isNotExpiredRoleAssignment(), is(true));
        assertThat(roleAssignments.getRoleAssignments().get(1).isNotExpiredRoleAssignment(), is(true));
    }

    @Test
    @DisplayName("shouldNotPassForIsAnExpiredRoleAssignment")
    void shouldNotPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 0;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        roleAssignments.getRoleAssignments().get(0).isNotExpiredRoleAssignment();
        assertThat(roleAssignments.getRoleAssignments().get(0).isNotExpiredRoleAssignment(), is(false));
        assertThat(roleAssignments.getRoleAssignments().get(1).isNotExpiredRoleAssignment(), is(false));
    }


    private RoleAssignments getRoleAssignments(final long oneHour) {

        final Instant currentTIme = Instant.now();
        final RoleAssignmentAttributes roleAssignmentAttributes =
            RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build();

        final List<RoleAssignment> roleAssignments = Arrays.asList(

            RoleAssignment.builder().actorId("actorId").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build(),

            RoleAssignment.builder().actorId("actorId1").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build()
        );
        return RoleAssignments.builder().roleAssignments(roleAssignments).build();
    }
}
