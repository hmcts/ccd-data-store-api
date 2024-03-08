package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRoleDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroups;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class CaseAccessGroupUtils {

    public static final String CASE_ACCESS_GROUPS = "CaseAccessGroups";

    protected static final String CCD_ALL_CASES = "CCD:all-cases-access";
    protected static final String ORGANISATION = "Organisation";
    protected static final String ORGANISATIONID = "OrganisationID";
    protected static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";
    protected static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    protected static final String CASE_ACCESS_GROUP_TYPE = "caseAccessGroupType";


    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {

        if (caseDetails.getData() != null && !caseDetails.getData().isEmpty()) {

            removeCCDAllCasesAccessFromCaseAccessGroups(caseDetails);

            List<AccessTypeRoleDefinition> accessTypeRoleDefinitions =
                caseTypeDefinition.getAccessTypeRoleDefinitions();
            List<AccessTypeRoleDefinition> filteredAccessTypeRolesDefinitions = filterAccessRoles(caseDetails,
                accessTypeRoleDefinitions);

            ObjectMapper mapper = new ObjectMapper();
            List<CaseAccessGroup> caseAccessGroups = new ArrayList<>();
            for (AccessTypeRoleDefinition acd : filteredAccessTypeRolesDefinitions) {
                JsonNode caseAssignedRoleFieldNode = findOrganisationPolicyNodeForCaseRole(caseDetails,
                    acd.getCaseAssignedRoleField());
                String orgIdentifier = caseAssignedRoleFieldNode.get(ORGANISATION).get(ORGANISATIONID).textValue();

                if (orgIdentifier != null) {
                    String caseGroupID = acd.getCaseAccessGroupIdTemplate()
                        .replace(ORG_IDENTIFIER_TEMPLATE, orgIdentifier);

                    CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseGroupID)
                        .caseAccessGroupType(CCD_ALL_CASES).build();

                    caseAccessGroups.add(caseAccessGroup);
                }

            }

            if (!caseAccessGroups.isEmpty()) {
                CaseAccessGroups groups = CaseAccessGroups.builder().caseAccessGroupsList(caseAccessGroups).build();
                JsonNode node = mapper.convertValue(groups, JsonNode.class);
                caseDetails.getData().put(CASE_ACCESS_GROUPS, node);
                groups.getCaseAccessGroupsList();
            }

        }

    }



    public JsonNode findOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        return caseDetails.getData().values().stream()
                .filter(node -> node.get(ORG_POLICY_CASE_ASSIGNED_ROLE) != null
                    && node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).asText().equalsIgnoreCase(caseRoleId))
                .reduce((a, b) -> {
                    throw new ValidationException(String.format("More than one Organisation Policy with "
                        + "case role ID '%s' exists on case", caseRoleId));
                })
                .orElseThrow(() -> new ValidationException(String.format("No Organisation Policy found with "
                    + "case role ID '%s'", caseRoleId)));
    }

    public boolean hasOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        return !findOrganisationPolicyNodeForCaseRole(caseDetails, caseRoleId).isEmpty();
    }

    private void removeCCDAllCasesAccessFromCaseAccessGroups(CaseDetails caseDetails) {
        ArrayNode caseAccessGroupsJsonNodes = (ArrayNode) caseDetails.getData().get(CASE_ACCESS_GROUPS);
        if (caseAccessGroupsJsonNodes != null && !caseAccessGroupsJsonNodes.isEmpty()) {
            for (int i = 0; i < caseAccessGroupsJsonNodes.size(); i++) {
                final JsonNode caseAccessGroupTypeValueNode = caseAccessGroupsJsonNodes.get(i)
                    .get(CASE_ACCESS_GROUP_TYPE);
                if (caseAccessGroupTypeValueNode != null
                    && caseAccessGroupTypeValueNode.textValue().equals(CCD_ALL_CASES)) {
                    caseAccessGroupsJsonNodes.remove(i);
                    i--;
                }
            }
            if (caseAccessGroupsJsonNodes.isEmpty()) {
                caseDetails.getData().remove(CASE_ACCESS_GROUPS);
            }
        }

    }

    private List<AccessTypeRoleDefinition> filterAccessRoles(
        CaseDetails caseDetails,
        List<AccessTypeRoleDefinition> accessTypeRolesDefinitions) {

        return accessTypeRolesDefinitions.stream()
            .filter(accessTypeRole -> StringUtils.isNoneBlank(accessTypeRole.getGroupRoleName())
                    && StringUtils.isNoneBlank(accessTypeRole.getCaseAssignedRoleField())
                    && hasOrganisationPolicyNodeForCaseRole(caseDetails, accessTypeRole.getCaseAssignedRoleField())
            )
            .toList();
    }
}
