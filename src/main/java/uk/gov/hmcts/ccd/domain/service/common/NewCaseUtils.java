package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Component
public class NewCaseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NewCaseUtils.class);

    private static final String ORGANISATION = "Organisation";
    private static final String ORGANISATIONID = "OrganisationID";
    public static final String ORG_POLICY_NEW_CASE = "newCase";
    public static final String SUPPLEMENTRY_DATA_NEW_CASE = "new_case";
    public static final String CASE_NEW_YES = "YES";
    public static final String CASE_NEW_NO = "NO";

    @Autowired
    public NewCaseUtils() {
    }

    public static void setupSupplementryDataWithNewCase(CaseDetails caseDetailsAfterCallbackWithoutHashes) {
        // Identify organizationProfiles with newCase set to (YES) true
        List<JsonNode> organizationProfiles
            = NewCaseUtils.findListOfOrganisationPolicyNodesForNewCase(caseDetailsAfterCallbackWithoutHashes,
            CASE_NEW_YES);

        // Update case supplementary data
        NewCaseUtils.updateCaseSupplementaryData(caseDetailsAfterCallbackWithoutHashes, organizationProfiles);

        // Clear organizationProfiles newCase attributes from case data
        NewCaseUtils.clearNewCaseAttributes(organizationProfiles);

        // Clear newCase attributes from case data if case_new set to No (false)
        clearNewCaseAttributesFromCaseDetailsSetToFalse(caseDetailsAfterCallbackWithoutHashes);
    }

    public static List<JsonNode> findListOfOrganisationPolicyNodesForNewCase(CaseDetails caseDetails,
                                                                             String newCaseValue) {
        if (caseDetails.getData() == null) {
            return Collections.EMPTY_LIST;
        }

        List<JsonNode> orgPolicyNewCaseNodes = Optional.ofNullable(caseDetails.getData().values())
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(Objects::nonNull)
            .filter(node -> node != null && node.get(ORG_POLICY_NEW_CASE) != null
                && node.get(ORG_POLICY_NEW_CASE).asText().toUpperCase().equals(newCaseValue))
            .collect(Collectors.toList());

        LOG.debug("Organisation found for  caseType={} version={} ORGANISATION={},"
                + "ORGANISATIONID={}, ORG_POLICY_CASE_ASSIGNED_ROLE={}.",
            caseDetails.getCaseTypeId(),caseDetails.getVersion(),
            ORGANISATION,ORGANISATIONID,ORG_POLICY_NEW_CASE, orgPolicyNewCaseNodes);
        return orgPolicyNewCaseNodes;
    }

    private static void updateCaseSupplementaryData(CaseDetails caseDetails, List<JsonNode> organizationProfiles) {

        ObjectNode orgNode = new ObjectMapper().createObjectNode();
        for (JsonNode orgProfile : organizationProfiles) {
            String orgIdentifier = orgProfile.get(ORGANISATION)
                .get(ORGANISATIONID).textValue();
            if (orgIdentifier != null && !orgIdentifier.isEmpty()) {
                orgNode.put(orgIdentifier, Boolean.TRUE.toString());
            }
        }

        if (!orgNode.isEmpty()) {
            Map<String, JsonNode> supplementaryData = caseDetails.getSupplementaryData();
            if (supplementaryData == null) {
                supplementaryData = new HashMap<>();
            }
            supplementaryData.put(SUPPLEMENTRY_DATA_NEW_CASE, orgNode);

            LOG.debug("new_case SupplementaryData ={} .", supplementaryData);
            caseDetails.setSupplementaryData(supplementaryData);
        }

    }

    private static void clearNewCaseAttributes(List<JsonNode> organizationProfiles) {
        for (JsonNode orgProfile : organizationProfiles) {
            ((ObjectNode) orgProfile).remove(ORG_POLICY_NEW_CASE);
        }
    }

    public static void clearNewCaseAttributesFromCaseDetailsSetToFalse(CaseDetails caseDetails) {
        // Identify organizationProfiles with newCase set to (No) false
        List<JsonNode> organizationProfiles
            = NewCaseUtils.findListOfOrganisationPolicyNodesForNewCase(caseDetails, CASE_NEW_NO);

        for (JsonNode orgProfile : organizationProfiles) {
            ((ObjectNode) orgProfile).remove(ORG_POLICY_NEW_CASE);
        }
    }
}
