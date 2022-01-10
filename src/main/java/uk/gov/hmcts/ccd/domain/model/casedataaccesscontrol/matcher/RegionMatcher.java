package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class RegionMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.REGION;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        String caseRegion = getRegion(caseDetails).orElse("");
        log.debug("Match role assignment region {} and case details region {} for role assignment {}",
            roleAssignment.getAttributes().getRegion(),
            caseRegion,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getRegion(), caseRegion);

        log.debug("Role assignment region {} and case details region {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseRegion,
            matched);
        return matched;
    }

    private Optional<String> getRegion(CaseDetails caseDetails) {
        JsonNode caseManagementLocation = caseDetails.getData().get(CASE_MANAGEMENT__LOCATION);
        if (caseManagementLocation != null && caseManagementLocation.get(REGION) != null) {
            return Optional.ofNullable(caseManagementLocation.get(REGION).asText());
        }
        return Optional.empty();
    }

}
