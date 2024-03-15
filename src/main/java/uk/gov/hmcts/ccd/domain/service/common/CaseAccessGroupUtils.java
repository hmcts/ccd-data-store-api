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
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseAccessGroupUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessGroupUtils.class);
    public static final String CASE_ACCESS_GROUPS = "CaseAccessGroups";

    protected static final String CCD_ALL_CASES = "CCD:all-cases-access";

    protected static final String ORGANISATION = "Organisation";
    protected static final String ORGANISATIONID = "OrganisationID";
    protected static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";
    protected static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    protected static final String CASE_ACCESS_GROUP_TYPE = "caseAccessGroupType";


    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {

        if (isCaseAcessGroupAvailable(caseDetails)) {

            removeCCDAllCasesAccessFromCaseAccessGroups(caseDetails);

            List<AccessTypeRoleDefinition> accessTypeRoleDefinitions =
                caseTypeDefinition.getAccessTypeRoleDefinitions();
            List<AccessTypeRoleDefinition> filteredAccessTypeRolesDefinitions = filterAccessRoles(caseDetails,
                accessTypeRoleDefinitions);
            if (filteredAccessTypeRolesDefinitions != null) {
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
                                .caseAccessGroup(caseAccessGroup).id(UUID.randomUUID().toString()).build();

                            caseAccessGroupForUIs.add(caseAccessGroupForUI);
                        }
                    }
                }
                setUpModifiedCaseAccessGroups(caseDetails, caseAccessGroupForUIs);
            }
        }

    }

    private void setUpModifiedCaseAccessGroups(CaseDetails caseDetails,
                                               List<CaseAccessGroupForUI> caseAccessGroupForUIs) {
        ObjectMapper mapper = new ObjectMapper();
        if (!caseAccessGroupForUIs.isEmpty()) {
            JsonNode caseAccessGroupForUIsNode = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
            if (caseDetails.getData().get(CASE_ACCESS_GROUPS) != null) {
                JsonNode caseDataCaseAccessGroup = caseDetails.getData().get(CASE_ACCESS_GROUPS);
                String mergedValue = caseDataCaseAccessGroup.toString() + caseAccessGroupForUIsNode.toString();
                mergedValue = mergedValue.replace("][",",");
                JsonNode mergedNode = null;
                try {
                    // remove the duplicate entry and convert string to json
                    mergedNode = new ObjectMapper().readTree(mergedValue);
                } catch (JsonProcessingException e) {
                    throw new ValidationException(String.format(e.getMessage()));
                }

                caseDetails.getData().put(CASE_ACCESS_GROUPS, mergedNode);

            } else {
                caseDetails.getData().put(CASE_ACCESS_GROUPS, caseAccessGroupForUIsNode);
            }

            LOG.debug("CASE_ACCESS_GROUPS = " + caseDetails.getData().get(CASE_ACCESS_GROUPS));
        }
    }

    private boolean isCaseAcessGroupAvailable(CaseDetails caseDetails) {
        return caseDetails.getData() != null
            && !caseDetails.getData().isEmpty()
            && caseDetails.getData().get(CASE_ACCESS_GROUPS) != null
            && !caseDetails.getData().get(CASE_ACCESS_GROUPS).isEmpty();
    }


    public JsonNode findOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        JsonNode caseRoleNode = caseDetails.getData().values().stream()
            .filter(node -> node.get(ORG_POLICY_CASE_ASSIGNED_ROLE) != null
                && node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).asText().equalsIgnoreCase(caseRoleId))
            .reduce((a, b) -> {
                LOG.info("No Organisation found for CASE_ACCESS_GROUPS={} caseType={} version={} ORGANISATION={},"
                    + "ORGANISATIONID={}, ORG_POLICY_CASE_ASSIGNED_ROLE={}.",
                    CASE_ACCESS_GROUPS,
                    caseDetails.getCaseTypeId(),caseDetails.getVersion(),
                    ORGANISATION,ORGANISATIONID,ORG_POLICY_CASE_ASSIGNED_ROLE);
                return null;
            }).orElse(null);

        LOG.debug("Organisation found for CASE_ACCESS_GROUPS={} caseType={} version={} ORGANISATION={},"
                + "ORGANISATIONID={}, ORG_POLICY_CASE_ASSIGNED_ROLE={}.",
            CASE_ACCESS_GROUPS,
            caseDetails.getCaseTypeId(),caseDetails.getVersion(),
            ORGANISATION,ORGANISATIONID,ORG_POLICY_CASE_ASSIGNED_ROLE);
        return caseRoleNode;
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
