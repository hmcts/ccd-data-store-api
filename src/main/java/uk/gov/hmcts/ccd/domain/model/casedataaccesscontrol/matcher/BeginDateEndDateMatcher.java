package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleMatchingResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Slf4j
@Component
public class BeginDateEndDateMatcher implements RoleAttributeMatcher {

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = resultPair.getLeft();
        log.debug("Apply filter on start {} and end time {} for role assignment {}",
            roleAssignment.getBeginTime(),
            roleAssignment.getEndTime(),
            roleAssignment.getId());
        RoleMatchingResult matchingResult = resultPair.getRight();
        if (roleAssignment.getBeginTime() != null && roleAssignment.getEndTime() != null) {
            Instant now = Instant.now();
            matchingResult.setDateMatched(roleAssignment.getBeginTime().compareTo(now) < 0
                && roleAssignment.getEndTime().compareTo(now) > 0);
        }
    }

    @Override
    public void matchAttribute(Pair<RoleAssignment, RoleMatchingResult> resultPair,
                               CaseTypeDefinition caseTypeDefinition) {
        resultPair.getRight().setDateMatched(true);
    }
}
