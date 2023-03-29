package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CaseDetailsToInvalidCaseSupplementaryDataItemMapper {
    public static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";

    public List<InvalidCaseSupplementaryDataItem> mapToDataItem(List<CaseDetails> caseDetails) {
        return caseDetails.stream().map(this::mapToDataItem).collect(Collectors.toList());
    }

    private InvalidCaseSupplementaryDataItem mapToDataItem(CaseDetails caseDetails) {
        Map<String, JsonNode> data = caseDetails.getData();
        String caseAccessCategory = JacksonUtils.getValueFromPath("CaseAccessCategory", data);

        List<JsonNode> orgPolicyNodes = findOrganisationPolicyNodes(caseDetails);
        Set<String> orgIds = orgPolicyNodes.stream()
            .map(e -> JacksonUtils.getValueFromPath("Organisation.OrganisationID", e))
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> orgCaseRoles = orgPolicyNodes.stream()
            .map(e -> JacksonUtils.getValueFromPath(ORG_POLICY_CASE_ASSIGNED_ROLE, e))
            .filter(Objects::nonNull).collect(Collectors.toSet());

        return InvalidCaseSupplementaryDataItem.builder()
            .caseId(caseDetails.getReference())
            .caseTypeId(caseDetails.getCaseTypeId())
            .jurisdiction(caseDetails.getJurisdiction())
            .supplementaryData(caseDetails.getSupplementaryData())
            .caseAccessCategory(caseAccessCategory)
            .organisationPolicyOrgIds(new ArrayList<>(orgIds))
            .orgPolicyCaseAssignedRoles(new ArrayList<>(orgCaseRoles))
            .build();
    }

    public List<JsonNode> findOrganisationPolicyNodes(CaseDetails caseDetails) {
        return caseDetails.getData().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
