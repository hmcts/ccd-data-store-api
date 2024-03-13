package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRoleDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroupForUI;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.ArrayList;
import java.util.List;

public class CaseAccessGroupUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessGroupUtils.class);
    public static final String CASE_ACCESS_GROUPS = "CaseAccessGroups";

    protected static final String CCD_ALL_CASES = "CCD:all-cases-access";
    protected static final String ORGANISATION_POLICY_FIELD = "OrganisationPolicyField";
    protected static final String ORGANISATION = "Organisation";
    protected static final String ORGANISATIONID = "OrganisationID";
    protected static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";
    protected static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    protected static final String CASE_ACCESS_GROUP_TYPE = "caseAccessGroupType";


    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {

        if (caseDetails.getData() != null
            && !caseDetails.getData().isEmpty()
            && !caseDetails.getData().get(CASE_ACCESS_GROUPS).isEmpty()) {

            removeCCDAllCasesAccessFromCaseAccessGroups(caseDetails);

            List<AccessTypeRoleDefinition> accessTypeRoleDefinitions =
                caseTypeDefinition.getAccessTypeRoleDefinitions();
            List<AccessTypeRoleDefinition> filteredAccessTypeRolesDefinitions = filterAccessRoles(caseDetails,
                accessTypeRoleDefinitions);
            if (filteredAccessTypeRolesDefinitions != null) {
                ObjectMapper mapper = new ObjectMapper();
                List<CaseAccessGroupForUI> caseAccessGroupForUIs = new ArrayList<>();
                for (AccessTypeRoleDefinition acd : filteredAccessTypeRolesDefinitions) {
                    JsonNode caseAssignedRoleFieldNode = findOrganisationPolicyNodeForCaseRole(caseDetails,
                        acd.getCaseAssignedRoleField());
                    if (caseAssignedRoleFieldNode != null) {
                        String orgIdentifier = caseAssignedRoleFieldNode.get(ORGANISATION)
                            .get(ORGANISATIONID).textValue();

                        if (orgIdentifier != null) {
                            String caseGroupID = acd.getCaseAccessGroupIdTemplate()
                                .replace(ORG_IDENTIFIER_TEMPLATE, orgIdentifier);

                            CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseGroupID)
                                .caseAccessGroupType(CCD_ALL_CASES).build();
                            CaseAccessGroupForUI caseAccessGroupForUI = CaseAccessGroupForUI.builder()
                                .caseAccessGroup(caseAccessGroup).id(1).build();

                            caseAccessGroupForUIs.add(caseAccessGroupForUI);
                        }
                    }
                }


                if (!caseAccessGroupForUIs.isEmpty()) {
                    //CaseAccessGroups groups = CaseAccessGroups.builder().caseAccessGroups(caseAccessGroups).build();
                    JsonNode caseAccessGroupForUIsNode = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
                    //caseDetails.getData().put(CASE_ACCESS_GROUPS, node); // this overwrites
                    //JacksonUtils.merge(nodes, caseDetails.getData()); //does not work
                    if (caseDetails.getData().get(CASE_ACCESS_GROUPS) != null) {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode caseDataCaseAccessGroup = caseDetails.getData().get(CASE_ACCESS_GROUPS);
                        String mergedValue = caseDataCaseAccessGroup.toString() + caseAccessGroupForUIsNode.toString();
                        mergedValue = mergedValue.replace("}{",",");
                        JsonNode mergedNode = null; // remove the duplicate entry and convert string to json
                        try {
                            mergedNode = new ObjectMapper().readTree(mergedValue);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        caseDetails.getData().put(CASE_ACCESS_GROUPS, mergedNode);

                    } else {
                        ObjectMapper objMapper = new ObjectMapper();
                        caseDetails.getData().put(CASE_ACCESS_GROUPS, caseAccessGroupForUIsNode);
                    }

                    //Output will be: {"k":"v","secondK":"secondV"}
                    LOG.debug("" + caseDetails.getData().get(CASE_ACCESS_GROUPS));
                }
            }
        }

    }



    public JsonNode findOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        JsonNode caseOrganisationPolicyFieldNode = caseDetails.getData().get(ORGANISATION_POLICY_FIELD);
        if (caseOrganisationPolicyFieldNode != null
            && !caseOrganisationPolicyFieldNode.isEmpty()
            && caseOrganisationPolicyFieldNode.get(ORG_POLICY_CASE_ASSIGNED_ROLE) != null
            && caseOrganisationPolicyFieldNode.get(ORG_POLICY_CASE_ASSIGNED_ROLE)
            .asText().equalsIgnoreCase(caseRoleId)) {
            return caseOrganisationPolicyFieldNode;
        }
        return null;
    }


    public boolean hasOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        JsonNode organisationPolicyNodeForCaseRole = findOrganisationPolicyNodeForCaseRole(caseDetails, caseRoleId);
        return (organisationPolicyNodeForCaseRole != null && !organisationPolicyNodeForCaseRole.isEmpty());
    }

    private void removeCCDAllCasesAccessFromCaseAccessGroups(CaseDetails caseDetails) {
        ArrayNode caseAccessGroupsJsonNodes = (ArrayNode) caseDetails.getData().get(CASE_ACCESS_GROUPS);
        if (caseAccessGroupsJsonNodes != null && !caseAccessGroupsJsonNodes.isEmpty()) {
            for (int i = 0; i < caseAccessGroupsJsonNodes.size(); i++) {
                JsonNode caseAccessGroupTypeValueNode = caseAccessGroupsJsonNodes.get(i);
                for (JsonNode field : caseAccessGroupTypeValueNode) {
                    if (field != null
                        && field.get(CASE_ACCESS_GROUP_TYPE) != null
                        && field.get(CASE_ACCESS_GROUP_TYPE).textValue().equals(CCD_ALL_CASES)) {
                        caseAccessGroupsJsonNodes.remove(i);
                        i--;
                        break;
                    }
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
