package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

@Mapper(componentModel = "spring")
public interface RoleAssignmentsMapper {

    RoleAssignmentsMapper INSTANCE = Mappers.getMapper(RoleAssignmentsMapper.class);

    RoleAssignments toRoleAssignments(RoleAssignmentResponse roleAssignmentResponse);
}
