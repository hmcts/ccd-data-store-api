package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.matcher;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseTypeHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Slf4j
@Component
public class SecurityClassificationMatcher implements RoleAttributeMatcher {

    @Override
    public MatcherType getType() {
        return MatcherType.SECURITYCLASSIFICATION;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseDetails caseDetails) {
        SecurityClassification caseDetailsSecurityClassification = caseDetails.getSecurityClassification();
        log.debug("Match role assignment security classification {} with case details security classification "
                + " {} for role assignment {}",
            roleAssignment.getClassification(),
            caseDetailsSecurityClassification,
            roleAssignment.getId());
        Optional<SecurityClassification> securityClassification = getSecurityClassification(roleAssignment
            .getClassification());
        boolean matched = false;
        if (securityClassification.isPresent()) {
            matched = caseHasClassificationEqualOrLowerThan(securityClassification.get()).test(caseDetails);
        }

        log.debug("Role assignment security classification {} and case details security classification "
                + " {} match {}",
            roleAssignment.getClassification(),
            caseDetailsSecurityClassification,
            matched);

        return matched;
    }

    @Override
    public boolean matchAttribute(RoleAssignment roleAssignment, CaseTypeDefinition caseTypeDefinition) {
        SecurityClassification caseTypeSecurityClassification = caseTypeDefinition.getSecurityClassification();
        log.debug("Match role assignment security classification {} with case type security classification "
                + " {} for role assignment {}",
            roleAssignment.getClassification(),
            caseTypeSecurityClassification,
            roleAssignment.getId());
        Optional<SecurityClassification> securityClassification = getSecurityClassification(roleAssignment
            .getClassification());
        boolean matched = false;
        if (securityClassification.isPresent()) {
            matched = caseTypeHasClassificationEqualOrLowerThan(securityClassification.get())
                .test(caseTypeDefinition);
        }

        log.debug("Role assignment security classification {} and case type security classification "
                + " {} match {}",
            roleAssignment.getClassification(),
            caseTypeSecurityClassification,
            matched);
        return matched;
    }

}
