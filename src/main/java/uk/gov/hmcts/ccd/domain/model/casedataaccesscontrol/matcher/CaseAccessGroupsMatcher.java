package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CaseAccessGroupsMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.ROLENAME;
    }

    private static final String VALUE_FIELD = "value";
    private static final String ID_FIELD = "id";
    private static final String CASE_GROUP_ID_FIELD = "caseGroupId";

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessGroupsMatcher.class);

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        List<String> caseAccessGroupIds = getGroupIds(caseDetails).orElse(Collections.EMPTY_LIST);
        LOG.info("Match role assignment caseGroupId {} with case details caseAccessGroupIds {} for role assignment {}",
            roleAssignment.getAttributes().getCaseGroupId(),
            caseAccessGroupIds,
            roleAssignment.getId());
        boolean matched = false;
        for (String caseAccessGroupId : caseAccessGroupIds) {
            if (isValuesMatching(roleAssignment.getAttributes().getCaseGroupId(), caseAccessGroupId)) {
                matched = true;
                break;
            }
        }

        log.info("Role assignment caseGroupId {} and case details caseAccessGroupIds {} match {}",
            roleAssignment.getAttributes().getCaseGroupId(),
            caseAccessGroupIds,
            matched);
        return matched;
    }

    private Optional<List> getGroupIds(CaseDetails caseDetails) {
        JsonNode caseAccessGroups = caseDetails.getData().get(CASE_ACCESS_GROUPS);
        if (caseAccessGroups != null) {
            return Optional.ofNullable(getCaseAccessGroupsCaseGroupIds(caseAccessGroups));
        }
        return Optional.empty();
    }

    private List<String> getCaseAccessGroupsCaseGroupIds(JsonNode caseAccessGroups) {

        List<String> allGroupIds = new ArrayList<>();
        if (caseAccessGroups.isArray()) {
            Iterator<JsonNode> elements = caseAccessGroups.elements();
            while (elements.hasNext()) {
                JsonNode jsonNode = elements.next();
                JsonNode valueNode = jsonNode.get(VALUE_FIELD);
                JsonNode idNode = jsonNode.get(ID_FIELD);
                if (valueNode != null && idNode != null) {
                    String caseGroupId = valueNode.get(CASE_GROUP_ID_FIELD).asText();
                    allGroupIds.add(caseGroupId);
                }
            }
        }

        return allGroupIds;
    }
}
