package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
public class SearchRoleAssignment {

    private String roleName;
    private String jurisdiction;
    private String caseType;
    @EqualsAndHashCode.Exclude
    private String caseReference;
    private String region;
    private String location;
    private String securityClassification;

    private String caseAccessGroupId;
    @EqualsAndHashCode.Exclude
    private RoleAssignment roleAssignment;

    public SearchRoleAssignment(RoleAssignment roleAssignment) {
        setRoleName(roleAssignment.getRoleName());
        setJurisdiction(defaultOptional(roleAssignment.getAttributes().getJurisdiction()));
        setCaseType(defaultOptional(roleAssignment.getAttributes().getCaseType()));
        setRegion(defaultOptional(roleAssignment.getAttributes().getRegion()));
        setLocation(defaultOptional(roleAssignment.getAttributes().getLocation()));
        setCaseReference(defaultOptional(roleAssignment.getAttributes().getCaseId()));
        setSecurityClassification(roleAssignment.getClassification());
        setCaseAccessGroupId(defaultOptional(roleAssignment.getAttributes().getCaseAccessGroupId()));
        setRoleAssignment(roleAssignment);
    }

    public boolean hasCaseReference() {
        return StringUtils.isNotBlank(caseReference);
    }

    @SuppressWarnings("java:S2789")
    private String defaultOptional(Optional<String> optional) {
        return optional == null ? EMPTY : optional.orElse(EMPTY);
    }
}
