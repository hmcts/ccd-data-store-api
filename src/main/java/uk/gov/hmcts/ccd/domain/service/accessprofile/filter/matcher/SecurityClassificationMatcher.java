package uk.gov.hmcts.ccd.domain.service.accessprofile.filter.matcher;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Slf4j
@Component
public class SecurityClassificationMatcher implements AttributeMatcher {

    @Override
    public boolean matchAttribute(RoleAssignmentFilteringResult result, CaseDetails caseDetails) {
        RoleAssignment roleAssignment = result.getRoleAssignment();
        log.debug("Apply filter on security classification {} for role assignment {}",
            roleAssignment.getClassification(),
            roleAssignment.getId());
        Optional<SecurityClassification> securityClassification = getSecurityClassification(roleAssignment
            .getClassification());
        if (securityClassification.isPresent()) {
            boolean value = caseHasClassificationEqualOrLowerThan(getSecurityClassification(roleAssignment
                .getClassification())
                .get())
                .test(caseDetails);
            result.getRoleMatchingResult().setValidClassification(value);
        }
        return result.getRoleMatchingResult().isValidClassification();
    }

}
