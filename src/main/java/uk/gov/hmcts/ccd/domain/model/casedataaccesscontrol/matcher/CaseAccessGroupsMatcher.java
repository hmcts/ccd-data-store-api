package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "enable-case-group-access-filtering", havingValue = "true")
public class CaseAccessGroupsMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.CASEACCESSGROUPID;
    }


    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        Optional<String> raCaseAccessGroupId = roleAssignment.getAttributes().getCaseAccessGroupId();
        List<String> caseAccessGroupIds = getCaseAccessGroupIds(caseDetails).orElse(Collections.emptyList());
        log.debug("Match role assignment caseAccessGroupId {} with caseAccessGroupIds {} for role assignment {}",
            raCaseAccessGroupId,
            caseAccessGroupIds,
            roleAssignment.getId());
        boolean matched = caseAccessGroupIds.isEmpty() && (ObjectUtils.isEmpty(raCaseAccessGroupId));

        for (String caseAccessGroupId : caseAccessGroupIds) {
            if (isValuesMatching(raCaseAccessGroupId, caseAccessGroupId)) {
                matched = true;
                break;
            }
        }

        log.debug("Role assignment caseAccessGroupId {} and case details caseAccessGroupIds {} match {}",
            raCaseAccessGroupId,
            caseAccessGroupIds,
            matched);
        return matched;
    }

    private Optional<List<String>> getCaseAccessGroupIds(CaseDetails caseDetails) {
        JsonNode caseAccessGroups = caseDetails.getData().get(CASE_ACCESS_GROUPS);
        if (caseAccessGroups != null && !caseAccessGroups.isEmpty()) {
            return Optional.ofNullable(getCaseAccessGroupIds(caseAccessGroups));
        }
        return Optional.empty();
    }

    private List<String> getCaseAccessGroupIds(JsonNode caseAccessGroups) {

        List<String> allGroupIds = new ArrayList<>();
        if (caseAccessGroups.isArray()) {
            Iterator<JsonNode> elements = caseAccessGroups.elements();
            while (elements.hasNext()) {
                JsonNode jsonNode = elements.next();
                JsonNode valueNode = jsonNode.get(COLLECTION_VALUE_FIELD);
                JsonNode idNode = jsonNode.get(COLLECTION_ID_FIELD);
                if (valueNode != null && idNode != null) {
                    String caseAccessGroupId = valueNode.get(CASE_ACCESS_GROUP_ID_FIELD).asText();
                    allGroupIds.add(caseAccessGroupId);
                }
            }
        }

        return allGroupIds;
    }
}
