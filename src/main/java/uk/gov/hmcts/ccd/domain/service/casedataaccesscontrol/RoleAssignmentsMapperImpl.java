package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentAttributesResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResource;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment.RoleAssignmentBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentAttributes.RoleAssignmentAttributesBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments.RoleAssignmentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleAssignmentsMapperImpl implements RoleAssignmentsMapper {

    @Override
    public RoleAssignments toRoleAssignments(RoleAssignmentResponse roleAssignmentResponse) {
        if (roleAssignmentResponse == null) {
            return null;
        }

        RoleAssignmentsBuilder roleAssignments = RoleAssignments.builder();

        roleAssignments.roleAssignments(roleAssignmentResourceListToRoleAssignmentList(
            roleAssignmentResponse.getRoleAssignments()));

        return roleAssignments.build();
    }

    protected RoleAssignmentAttributes roleAssignmentAttributesResourceToRoleAssignmentAttributes(
        RoleAssignmentAttributesResource roleAssignmentAttributesResource) {
        if (roleAssignmentAttributesResource == null) {
            return null;
        }

        RoleAssignmentAttributesBuilder roleAssignmentAttributes = RoleAssignmentAttributes.builder();

        roleAssignmentAttributes.jurisdiction(roleAssignmentAttributesResource.getJurisdiction());
        roleAssignmentAttributes.caseId(roleAssignmentAttributesResource.getCaseId());
        roleAssignmentAttributes.region(roleAssignmentAttributesResource.getRegion());
        roleAssignmentAttributes.location(roleAssignmentAttributesResource.getLocation());
        roleAssignmentAttributes.contractType(roleAssignmentAttributesResource.getContractType());

        return roleAssignmentAttributes.build();
    }

    protected RoleAssignment roleAssignmentResourceToRoleAssignment(RoleAssignmentResource roleAssignmentResource) {
        if (roleAssignmentResource == null) {
            return null;
        }

        RoleAssignmentBuilder roleAssignment = RoleAssignment.builder();

        roleAssignment.id(roleAssignmentResource.getId());
        roleAssignment.actorIdType(roleAssignmentResource.getActorIdType());
        roleAssignment.actorId(roleAssignmentResource.getActorId());
        roleAssignment.roleType(roleAssignmentResource.getRoleType());
        roleAssignment.roleName(roleAssignmentResource.getRoleName());
        roleAssignment.classification(roleAssignmentResource.getClassification());
        roleAssignment.grantType(roleAssignmentResource.getGrantType());
        roleAssignment.roleCategory(roleAssignmentResource.getRoleCategory());
        roleAssignment.readOnly(roleAssignmentResource.getReadOnly());
        roleAssignment.beginTime(roleAssignmentResource.getBeginTime());
        roleAssignment.endTime(roleAssignmentResource.getEndTime());
        roleAssignment.created(roleAssignmentResource.getCreated());
        List<String> list = roleAssignmentResource.getAuthorisations();
        if (list != null) {
            roleAssignment.authorisations(new ArrayList<String>(list));
        }
        roleAssignment.attributes(roleAssignmentAttributesResourceToRoleAssignmentAttributes(
            roleAssignmentResource.getAttributes()));

        return roleAssignment.build();
    }

    protected List<RoleAssignment> roleAssignmentResourceListToRoleAssignmentList(List<RoleAssignmentResource> list) {
        if (list == null) {
            return null;
        }

        List<RoleAssignment> list1 = new ArrayList<RoleAssignment>(list.size());
        for (RoleAssignmentResource roleAssignmentResource : list) {
            list1.add(roleAssignmentResourceToRoleAssignment(roleAssignmentResource));
        }

        return list1;
    }
}

