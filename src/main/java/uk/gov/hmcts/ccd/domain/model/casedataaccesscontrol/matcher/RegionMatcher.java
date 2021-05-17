package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class RegionMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPai, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = resultPai.getLeft();
        String caseRegion = getRegion(caseDetails).orElse("");
        log.debug("Match role assignment region {} and case details region {} for role assignment {}",
            roleAssignment.getAttributes().getRegion(),
            caseRegion,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getRegion(), caseRegion);
        resultPai.getRight()
            .setRegionMatched(matched);

        log.debug("Role assignment region {} and case details region {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseRegion,
            matched);
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {
        // TODO : need to implement this for search and create cases
    }

    private Optional<String> getRegion(CaseDetails caseDetails) {
        JsonNode caseManagementLocation = caseDetails.getData().get("caseManagementLocation");
        if (caseManagementLocation != null) {
            return Optional.ofNullable(caseManagementLocation.get("region").asText());
        }
        return Optional.empty();
    }

}
