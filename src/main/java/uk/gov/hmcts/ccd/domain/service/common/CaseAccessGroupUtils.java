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
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroupWithId;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
    protected static final String GROUPACCESS_VALUE = "value";

    private CaseDataService caseDataService;
    private CaseTypeDefinition caseTypeDefinition;

    protected static final String GROUPACCESS_CLASSIFICATION = "classification";

    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails,
                                                    CaseTypeDefinition caseTypeDefinition,
                                                    CaseDataService caseDataService) {
        this.caseDataService = caseDataService;
        this.caseTypeDefinition = caseTypeDefinition;

        removeCCDAllCasesAccessFromCaseAccessGroups(caseDetails);

        List<AccessTypeRoleDefinition> accessTypeRoleDefinitions =
            caseTypeDefinition.getAccessTypeRoleDefinitions();
        List<AccessTypeRoleDefinition> filteredAccessTypeRolesDefinitions = filterAccessRoles(caseDetails,
            accessTypeRoleDefinitions);
        if (filteredAccessTypeRolesDefinitions != null) {
            List<CaseAccessGroupWithId> caseAccessGroupWithIds = new ArrayList<>();
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
                        CaseAccessGroupWithId caseAccessGroupWithId = CaseAccessGroupWithId.builder()
                            .caseAccessGroup(caseAccessGroup).id(UUID.randomUUID().toString()).build();

                        caseAccessGroupWithIds.add(caseAccessGroupWithId);
                    }
                }
            }
            setUpModifiedCaseAccessGroups(caseDetails, caseAccessGroupWithIds);
        }

    }

    private void setUpModifiedCaseAccessGroups(CaseDetails caseDetails,
                                               List<CaseAccessGroupWithId> caseAccessGroupWithIds) {
        ObjectMapper mapper = new ObjectMapper();
        if (!caseAccessGroupWithIds.isEmpty()) {
            JsonNode caseAccessGroupWithIdsNode = mapper.convertValue(caseAccessGroupWithIds, JsonNode.class);
            if (caseDetails.getData().get(CASE_ACCESS_GROUPS) != null) {
                JsonNode caseDataCaseAccessGroup = caseDetails.getData().get(CASE_ACCESS_GROUPS);

                String mergedValue = null;
                if (caseDataCaseAccessGroup != null && !caseDataCaseAccessGroup.isEmpty()) {
                    mergedValue = caseDataCaseAccessGroup.toString() + caseAccessGroupWithIdsNode.toString();
                    mergedValue = mergedValue.replace("][",",");
                } else {
                    mergedValue = caseAccessGroupWithIdsNode.toString();
                }

                JsonNode mergedNode = null;
                try {
                    mergedNode = new ObjectMapper().readTree(mergedValue);
                } catch (JsonProcessingException e) {
                    throw new ValidationException(String.format(e.getMessage()));
                }

                caseDetails.getData().put(CASE_ACCESS_GROUPS, mergedNode);

            } else {
                caseDetails.getData().put(CASE_ACCESS_GROUPS, caseAccessGroupWithIdsNode);
            }

            LOG.debug("CASE ACCESS GROUPS : {} ", caseDetails.getData().get(CASE_ACCESS_GROUPS));

            JsonNode caseAccessGroupJsonNode = caseDetails.getData().get(CASE_ACCESS_GROUPS);
            Map<String,JsonNode> caseDataWithCaseAccessGroup = Map.of(CASE_ACCESS_GROUPS, caseAccessGroupJsonNode);
            Map<String,JsonNode> caseDataClassificationWithCaseAccessGroup =
                updateCaseDataClassificationWithCaseGroupAccess(caseDataWithCaseAccessGroup,
                caseDetails.getDataClassification(),
                caseDataService,
                caseTypeDefinition);
            caseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);
        }
    }

    public Map<String, JsonNode> updateCaseDataClassificationWithCaseGroupAccess(
        Map<String, JsonNode> caseAccessGroupdata, Map<String, JsonNode> dataClassification,
        CaseDataService caseDataService, CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> outputDataClassification = null;
        if (caseAccessGroupdata != null && !caseAccessGroupdata.isEmpty()) {
            // generate just the CaseAccessGroups data classification from just CaseAccessGroups field data
            Map<String, JsonNode> justCaseAccessGroupsDataClassification =
                caseDataService.getDefaultSecurityClassifications(
                caseTypeDefinition,
                Map.of(CASE_ACCESS_GROUPS, caseAccessGroupdata.get(CASE_ACCESS_GROUPS)),
                new HashMap<>()
            );

            // .. then clone current data classification and set the CaseAccessGroup classification
            outputDataClassification = cloneOrNewJsonMap(dataClassification);
            outputDataClassification.put(CASE_ACCESS_GROUPS,
                justCaseAccessGroupsDataClassification.get(CASE_ACCESS_GROUPS));
        }
        LOG.debug("CaseAccessGroup data classification : {} ", outputDataClassification);
        return outputDataClassification;
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
        if (isAccessGroupsJsonNodesAvailable(caseAccessGroupsJsonNodes)) {
            for (int i = 0; i < caseAccessGroupsJsonNodes.size(); i++) {
                JsonNode caseAccessGroupTypeValueNode = caseAccessGroupsJsonNodes.get(i);
                String idToRemove = null;
                if (hasNodeValueAndId(caseAccessGroupTypeValueNode)) {
                    idToRemove = caseAccessGroupTypeValueNode.get("id").textValue();
                    for (JsonNode field : caseAccessGroupTypeValueNode) {
                        if (isCaseAccessGroupTypeField(field)) {
                            caseAccessGroupsJsonNodes.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
            removeCaseAccessGroup(caseAccessGroupsJsonNodes, caseDetails);
        }
    }

    private void removeCaseAccessGroup(ArrayNode caseAccessGroupsJsonNodes, CaseDetails caseDetails) {

        if (caseAccessGroupsJsonNodes.isEmpty()) {
            caseDetails.getData().remove(CASE_ACCESS_GROUPS);
        }
    }

    private boolean isAccessGroupsJsonNodesAvailable(ArrayNode caseAccessGroupsJsonNodes) {
        return (caseAccessGroupsJsonNodes != null && !caseAccessGroupsJsonNodes.isEmpty());
    }

    private boolean hasNodeValueAndId(JsonNode caseAccessGroupTypeValueNode) {
        return (caseAccessGroupTypeValueNode.get("id") != null
            && caseAccessGroupTypeValueNode.get("value") != null);
    }

    private boolean isCaseAccessGroupTypeField(JsonNode field) {
        return (field != null
            && field.get(CASE_ACCESS_GROUP_TYPE) != null
            && field.get(CASE_ACCESS_GROUP_TYPE).textValue().equals(CCD_ALL_CASES));
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

    /*public Map<String, JsonNode>
        updateCaseDataClassificationWithCaseGroupAccess(SecurityClassification securityClassification,
                                                        Map<String, JsonNode> data,
                                                        Map<String, JsonNode> dataClassification,
                                                        JsonNode justCaseAccessGroupDataClassification) {

        Map<String, JsonNode> outputDataClassification = dataClassification;

        // .. then clone current data classification and set the CaseAccessGroup classification
        outputDataClassification = cloneOrNewJsonMap(dataClassification);
        if (justCaseAccessGroupDataClassification != null && !justCaseAccessGroupDataClassification.isEmpty()) {
            ObjectMapper mapper = new JsonMapper();

            ObjectNode groupAccessNode = mapper.createObjectNode();
            groupAccessNode.put(GROUPACCESS_CLASSIFICATION, securityClassification.name());
            groupAccessNode.put(GROUPACCESS_VALUE, justCaseAccessGroupDataClassification);

            ObjectNode root = mapper.createObjectNode();
            root.put(CASE_ACCESS_GROUPS,groupAccessNode);
            outputDataClassification = mapper.convertValue(root, Map.class);

        }

        return outputDataClassification;
    }*/

    private Map<String, JsonNode> cloneOrNewJsonMap(Map<String, JsonNode> jsonMap) {
        if (jsonMap != null) {
            // shallow clone
            return new HashMap<>(jsonMap);
        } else {
            return new HashMap<>();
        }
    }

}
