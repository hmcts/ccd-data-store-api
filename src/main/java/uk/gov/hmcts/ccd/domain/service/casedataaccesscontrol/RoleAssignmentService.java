package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRecord;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRecordAttribute;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.data.roleassignment.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.definition.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.RoleAssignmentAttribute;
import uk.gov.hmcts.ccd.domain.service.AccessControl;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService implements AccessControl {

    private final RoleAssignmentRepository roleAssignmentRepository;

    @Autowired
    public RoleAssignmentService(RoleAssignmentRepository roleAssignmentRepository) {
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    public List<RoleAssignment> getRoleAssignments(String userId) {
        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentRepository.getRoleAssignments(userId);
        return mapToRoleAssignments(roleAssignmentResponse);
    }

    private List<RoleAssignment> mapToRoleAssignments(RoleAssignmentResponse roleAssignmentResponse) {
        return roleAssignmentResponse.getRoleAssignmentRecords().stream()
            .map(this::mapToRoleAssignment)
            .collect(Collectors.toList());
    }

    private RoleAssignment mapToRoleAssignment(RoleAssignmentRecord roleAssignmentRecord) {
        return RoleAssignment.builder()
            .id(roleAssignmentRecord.getId())
            .actorIdType(roleAssignmentRecord.getActorIdType())
            .actorId(roleAssignmentRecord.getActorId())
            .roleType(roleAssignmentRecord.getRoleType())
            .roleName(roleAssignmentRecord.getRoleName())
            .classification(roleAssignmentRecord.getClassification())
            .grantType(roleAssignmentRecord.getGrantType())
            .roleCategory(roleAssignmentRecord.getRoleCategory())
            .readOnly(roleAssignmentRecord.getReadOnly())
            .beginTime(roleAssignmentRecord.getBeginTime())
            .endTime(roleAssignmentRecord.getEndTime())
            .created(roleAssignmentRecord.getCreated())
            .authorisations(roleAssignmentRecord.getAuthorisations())
            .attributes(mapToRoleAssignmentAttribute(roleAssignmentRecord.getAttributes()))
            .build();
    }

    private List<RoleAssignmentAttribute> mapToRoleAssignmentAttribute(List<RoleAssignmentRecordAttribute> attributes) {
        return attributes.stream()
            .map(this::mapToRoleAssignmentAttribute)
            .collect(Collectors.toList());
    }

    private RoleAssignmentAttribute mapToRoleAssignmentAttribute(RoleAssignmentRecordAttribute attribute) {
        return RoleAssignmentAttribute.builder()
            .jurisdiction(attribute.getJurisdiction())
            .caseId(attribute.getCaseId())
            .region(attribute.getRegion())
            .location(attribute.getLocation())
            .contractType(attribute.getContractType())
            .build();
    }
}
