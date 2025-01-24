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
import java.util.ArrayList;
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

    @Autowired
    public NewCaseUtils() {
    }

    public static List<JsonNode> findListOfOrganisationPolicyNodesForNewCase(CaseDetails caseDetails) {

        List<JsonNode> orgPolicyNewCaseNodes = Optional.ofNullable(caseDetails.getData().values())
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(Objects::nonNull)
            .filter(node -> node != null && node.get(ORG_POLICY_NEW_CASE) != null
                && node.get(ORG_POLICY_NEW_CASE).asText().equals(Boolean.TRUE.toString()))
            .collect(Collectors.toList());

        LOG.debug("Organisation found for  caseType={} version={} ORGANISATION={},"
                + "ORGANISATIONID={}, ORG_POLICY_CASE_ASSIGNED_ROLE={}.",
            caseDetails.getCaseTypeId(),caseDetails.getVersion(),
            ORGANISATION,ORGANISATIONID,ORG_POLICY_NEW_CASE, orgPolicyNewCaseNodes);
        return orgPolicyNewCaseNodes;
    }

    public static void updateCaseSupplementaryData(CaseDetails caseDetails, List<JsonNode> organizationProfiles) {
        Map<String, JsonNode> supplementaryData = caseDetails.getSupplementaryData();
        List<JsonNode> orgIdList = new ArrayList<>();

        for (JsonNode orgProfile : organizationProfiles) {
            if (supplementaryData == null) {
                supplementaryData = new HashMap<>();
            }
            String orgIdentifier = orgProfile.get(ORGANISATION)
                .get(ORGANISATIONID).textValue();

            JsonNode orgNode = new ObjectMapper().createObjectNode()
                .put(orgIdentifier, Boolean.TRUE.toString());
            orgIdList.add(orgNode);
        }

        if (!orgIdList.isEmpty()) {
            JsonNode jsonNode = new ObjectMapper().createArrayNode().addAll(orgIdList);
            supplementaryData.put(SUPPLEMENTRY_DATA_NEW_CASE, jsonNode);
        }
        LOG.debug("SupplementaryData ={} .", supplementaryData);
        caseDetails.setSupplementaryData(supplementaryData);
    }

    public static void clearNewCaseAttributes(List<JsonNode> organizationProfiles) {
        for (JsonNode orgProfile : organizationProfiles) {
            ((ObjectNode) orgProfile).remove(ORG_POLICY_NEW_CASE);
        }
    }
}
