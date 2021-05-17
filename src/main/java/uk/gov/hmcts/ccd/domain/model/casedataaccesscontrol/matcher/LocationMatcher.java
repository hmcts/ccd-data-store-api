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
public class LocationMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPai, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = resultPai.getLeft();
        String caseLocation = getLocation(caseDetails).orElse("");
        log.debug("Match role assignment location {} with case details location {} for role assignment {}",
            roleAssignment.getAttributes().getLocation(),
            caseLocation,
            roleAssignment.getId());
        boolean matched = isValuesMatching(roleAssignment.getAttributes().getLocation(), caseLocation);
        resultPai.getRight()
            .setLocationMatched(matched);

        log.debug("Role assignment location {} and case details location {} match {}",
            roleAssignment.getAttributes().getLocation(),
            caseLocation,
            matched);
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {
        // TODO : need to implement this for search and create cases
    }

    private Optional<String> getLocation(CaseDetails caseDetails) {
        JsonNode caseManagementLocation = caseDetails.getData().get("caseManagementLocation");
        if (caseManagementLocation != null) {
            return Optional.ofNullable(caseManagementLocation.get("baseLocation").asText());
        }
        return Optional.empty();
    }

}
