package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Slf4j
@Component
public class LocationMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.LOCATION;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        String caseLocation = getLocation(caseDetails).orElse(EMPTY_STR);
        log.debug("Match role assignment location {} with case details location {} for role assignment {}",
            roleAssignment.getAttributes().getLocation(),
            caseLocation,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getLocation(), caseLocation);

        log.debug("Role assignment location {} and case details location {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseLocation,
            matched);
        return matched;
    }

    private Optional<String> getLocation(CaseDetails caseDetails) {
        JsonNode caseManagementLocation = caseDetails.getData().get(CASE_MANAGEMENT__LOCATION);
        if (caseManagementLocation != null && caseManagementLocation.get(BASE_LOCATION) != null) {
            return Optional.ofNullable(caseManagementLocation.get(BASE_LOCATION).asText());
        }
        return Optional.empty();
    }

}
