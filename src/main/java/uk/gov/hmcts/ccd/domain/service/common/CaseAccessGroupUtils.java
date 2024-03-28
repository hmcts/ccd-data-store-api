package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

@Component
public class CaseAccessGroupUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessGroupUtils.class);

    public static final String CASE_ACCESS_GROUPS = "CaseAccessGroups";
    public static final String CCD_ALL_CASES = "CCD:all-cases-access";

    private static final String ORGANISATION = "Organisation";
    private static final String ORGANISATIONID = "OrganisationID";
    private static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";
    private static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    private static final String CASE_ACCESS_GROUP_TYPE = "caseAccessGroupType";
    private static final String GROUPACCESS_VALUE = "value";
    private static final String GROUPACCESS_ID = "id";
    private CaseDataService caseDataService;
    private ObjectMapper objectMapper;

    @Autowired
    public CaseAccessGroupUtils(CaseDataService caseDataService, ObjectMapper objectMapper) {
        this.caseDataService = caseDataService;
        this.objectMapper = objectMapper;
    }

    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {

        removeCCDAllCasesAccessFromFromCaseDataCaseAccessGroups(caseDetails);

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

                    if (orgIdentifier != null && !orgIdentifier.isEmpty()) {
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
            setUpModifiedCaseAccessGroups(caseDetails, caseAccessGroupWithIds, caseTypeDefinition);
        }

    }

    public Map<String, JsonNode> updateCaseDataClassificationWithCaseGroupAccess(
        CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> dataClassification = caseDetails.getDataClassification();
        JsonNode caseAccessGroupJsonNode = caseDetails.getData().get(CASE_ACCESS_GROUPS);

        Map<String,JsonNode> caseAccessGroupdata = null;
        if (caseAccessGroupJsonNode != null) {
            caseAccessGroupdata = Map.of(CASE_ACCESS_GROUPS, caseAccessGroupJsonNode);
        }
        Map<String, JsonNode> outputDataClassification = caseDetails.getDataClassification();
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

    private void setUpModifiedCaseAccessGroups(CaseDetails caseDetails,
                                               List<CaseAccessGroupWithId> caseAccessGroupWithIds,
                                               CaseTypeDefinition caseTypeDefinition) {

        if (!caseAccessGroupWithIds.isEmpty()) {
            JsonNode caseAccessGroupWithIdsNode = objectMapper.convertValue(caseAccessGroupWithIds, JsonNode.class);
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
                    mergedNode = objectMapper.readTree(mergedValue);
                } catch (JsonProcessingException e) {
                    throw new ValidationException(String.format(e.getMessage()));
                }

                caseDetails.getData().put(CASE_ACCESS_GROUPS, mergedNode);

            } else {
                caseDetails.getData().put(CASE_ACCESS_GROUPS, caseAccessGroupWithIdsNode);
            }

            LOG.debug("CASE ACCESS GROUPS : {} ", caseDetails.getData().get(CASE_ACCESS_GROUPS));
        }

        Map<String,JsonNode> caseDataClassificationWithCaseAccessGroup =
            updateCaseDataClassificationWithCaseGroupAccess(caseDetails, caseTypeDefinition);
        caseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);
    }

    public JsonNode findOrganisationPolicyNodeForCaseRole(CaseDetails caseDetails, String caseRoleId) {
        JsonNode caseRoleNode = caseDetails.getData().values().stream()
            .filter(node -> node.get(ORG_POLICY_CASE_ASSIGNED_ROLE) != null
                && node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).asText().equalsIgnoreCase(caseRoleId))
            .reduce((a, b) -> {
                LOG.debug("No Organisation found for CASE_ACCESS_GROUPS={} caseType={} version={} ORGANISATION={},"
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

    private void removeCCDAllCasesAccessFromFromCaseDataCaseAccessGroups(CaseDetails caseDetails) {
        ArrayNode caseAccessGroupsJsonNodes = (ArrayNode) caseDetails.getData().get(CASE_ACCESS_GROUPS);
        if (isAccessGroupsJsonNodesAvailable(caseAccessGroupsJsonNodes)) {
            for (int i = 0; i < caseAccessGroupsJsonNodes.size(); i++) {
                JsonNode caseAccessGroupTypeValueNode = caseAccessGroupsJsonNodes.get(i);
                if (hasNodeValueAndId(caseAccessGroupTypeValueNode)) {
                    for (JsonNode field : caseAccessGroupTypeValueNode) {
                        if (isCaseAccessGroupTypeField(field)) {
                            caseAccessGroupsJsonNodes.remove(i);
                            i--;
                            break;
                        }
                    }
                }
            }
        }
        removeCaseAccessGroupFromCaseData(caseAccessGroupsJsonNodes, caseDetails);
    }

    private void removeCaseAccessGroupFromCaseData(ArrayNode caseAccessGroupsJsonNodes, CaseDetails caseDetails) {
        if (caseAccessGroupsJsonNodes != null && caseAccessGroupsJsonNodes.isEmpty()) {
            caseDetails.getData().remove(CASE_ACCESS_GROUPS);

            if (isCaseAccessGroupFromDataClassificationAvailable(caseDetails)) {
                caseDetails.getDataClassification().remove(CASE_ACCESS_GROUPS);
            }
        }
    }

    private boolean isAccessGroupsJsonNodesAvailable(ArrayNode caseAccessGroupsJsonNodes) {
        return (caseAccessGroupsJsonNodes != null && !caseAccessGroupsJsonNodes.isEmpty());
    }

    private boolean hasNodeValueAndId(JsonNode caseAccessGroupTypeValueNode) {
        return (caseAccessGroupTypeValueNode.get(GROUPACCESS_ID) != null
            && caseAccessGroupTypeValueNode.get(GROUPACCESS_VALUE) != null);
    }

    private boolean isCaseAccessGroupTypeField(JsonNode field) {
        return (field != null
            && field.get(CASE_ACCESS_GROUP_TYPE) != null
            && field.get(CASE_ACCESS_GROUP_TYPE).textValue().equals(CCD_ALL_CASES));
    }

    private boolean isCaseAccessGroupFromDataClassificationAvailable(CaseDetails caseDetails) {
        return (caseDetails.getDataClassification() != null
            && caseDetails.getDataClassification().get(CASE_ACCESS_GROUPS) != null);
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

    private Map<String, JsonNode> cloneOrNewJsonMap(Map<String, JsonNode> jsonMap) {
        if (jsonMap != null) {
            // shallow clone
            return new HashMap<>(jsonMap);
        } else {
            return new HashMap<>();
        }
    }
}
