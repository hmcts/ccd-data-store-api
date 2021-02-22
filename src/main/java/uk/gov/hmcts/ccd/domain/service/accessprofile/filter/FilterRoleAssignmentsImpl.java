package uk.gov.hmcts.ccd.domain.service.accessprofile.filter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils;

import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;

@Slf4j
@Component
public class FilterRoleAssignmentsImpl implements FilterRoleAssignments, AccessControl {

    @Override
    public RoleAssignments filter(RoleAssignments roleAssignments, CaseDetails caseDetails) {
        log.info("Filter role assignments for case {}", caseDetails.getReference());

        List<RoleAssignment> roleAssignmentList = roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> validateStartAndEndDate(roleAssignment))
            .filter(roleAssignment -> validateSecurityValidation(caseDetails, roleAssignment))
            .filter(roleAssignment -> validateCaseId(caseDetails, roleAssignment))
            .filter(roleAssignment -> validateJurisdiction(caseDetails, roleAssignment))
            .filter(roleAssignment -> validateLocation(roleAssignment))
            .filter(roleAssignment -> validateRegion(roleAssignment))
            .collect(Collectors.toList());

        return RoleAssignments.builder().roleAssignments(roleAssignmentList).build();
    }

    @Override
    public RoleAssignments filter(RoleAssignments roleAssignments, CaseDataContent caseDataContent) {
        log.info("Filter role assignments for case event {}", caseDataContent.getEvent().getEventId());

        List<RoleAssignment> roleAssignmentList = roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> validateStartAndEndDate(roleAssignment))
            .collect(Collectors.toList());
        return RoleAssignments.builder().roleAssignments(roleAssignmentList).build();
    }

    @Override
    public RoleAssignments filter(RoleAssignments roleAssignments, CaseTypeDefinition caseTypeDefinition) {
        log.info("Filter role assignments for case type {}", caseTypeDefinition.getName());

        List<RoleAssignment> roleAssignmentList = roleAssignments.getRoleAssignments().stream()
            .filter(roleAssignment -> validateStartAndEndDate(roleAssignment))
            .collect(Collectors.toList());
        return RoleAssignments.builder().roleAssignments(roleAssignmentList).build();
    }

    private boolean validateJurisdiction(CaseDetails caseDetails, RoleAssignment roleAssignment) {
        log.debug("Apply filter on jurisdiction {} for role assignment {}",
            caseDetails.getJurisdiction(),
            roleAssignment.getId());
        roleAssignment.getMatchingResults().setValidJurisdiction(
            roleAssignment.getAttributes().getJurisdiction()
                .equals(caseDetails.getJurisdiction()));
        return roleAssignment.getMatchingResults().isValidJurisdiction();
    }

    private boolean validateSecurityValidation(CaseDetails caseDetails, RoleAssignment roleAssignment) {
        log.debug("Apply filter on security classification {} for role assignment {}",
            roleAssignment.getClassification(),
            roleAssignment.getId());
        Optional<SecurityClassification> securityClassification = SecurityClassificationUtils
            .getSecurityClassification(roleAssignment
                .getClassification());
        if (securityClassification.isPresent()) {
            boolean value = caseHasClassificationEqualOrLowerThan(SecurityClassificationUtils
                .getSecurityClassification(roleAssignment
                    .getClassification())
                .get())
                .test(caseDetails);
            roleAssignment.getMatchingResults().setValidClassification(value);
        }
        return roleAssignment.getMatchingResults().isValidClassification();
    }

    private boolean validateCaseId(CaseDetails caseDetails, RoleAssignment roleAssignment) {
        log.debug("Apply filter on case id {} for role assignment {}",
            roleAssignment.getAttributes().getCaseId(),
            roleAssignment.getId());
        roleAssignment.getMatchingResults().setValidCaseId(
            roleAssignment.getAttributes().getCaseId().equals(caseDetails.getReferenceAsString()));
        return roleAssignment.getMatchingResults().isValidCaseId();
    }

    private boolean validateLocation(RoleAssignment roleAssignment) {
        log.debug("Apply filter on location {} for role assignment {}",
            roleAssignment.getAttributes().getLocation(),
            roleAssignment.getId());
        boolean isEmpty = StringUtils.isEmpty(roleAssignment.getAttributes().getLocation());
        if (isEmpty) {
            roleAssignment.getMatchingResults().setValidLocation(true);
        } else {
            roleAssignment.getMatchingResults().setValidLocation(
                roleAssignment.getAttributes().getLocation()
                    .equals("" /* location */));
        }
        return roleAssignment.getMatchingResults().isValidLocation();
    }

    private boolean validateRegion(RoleAssignment roleAssignment) {
        log.debug("Apply filter on region {} for role assignment {}",
            roleAssignment.getAttributes().getRegion(),
            roleAssignment.getId());
        boolean isEmpty = StringUtils.isEmpty(roleAssignment.getAttributes().getRegion());
        if (isEmpty) {
            roleAssignment.getMatchingResults().setValidRegion(true);
        } else {
            roleAssignment.getMatchingResults().setValidRegion(
                roleAssignment.getAttributes().getRegion()
                    .equals("" /* region */));
        }
        return roleAssignment.getMatchingResults().isValidRegion();
    }

    private boolean validateStartAndEndDate(RoleAssignment roleAssignment) {
        log.debug("Apply filter on start {} and end time {} for role assignment {}",
            roleAssignment.getBeginTime(),
            roleAssignment.getEndTime(),
            roleAssignment.getId());
        Instant now = Instant.now();
        boolean value = roleAssignment.getBeginTime().compareTo(now) < 0
            && roleAssignment.getEndTime().compareTo(now) > 0;
        roleAssignment.getMatchingResults().setValidDate(value);
        return value;
    }
}
