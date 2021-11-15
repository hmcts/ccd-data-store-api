package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import java.util.Optional;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Data
public class SearchRoleAssignment {

    private String jurisdiction;
    private String caseType;
    @EqualsAndHashCode.Exclude
    private String caseReference;
    private String region;
    private String location;
    private String securityClassification;
    private Set<String> readableCaseStates;

    public SearchRoleAssignment(RoleAssignment roleAssignment, Set<String> readableCaseStates) {
        setJurisdiction(defaultOptional(roleAssignment.getAttributes().getJurisdiction()));
        setCaseType(defaultOptional(roleAssignment.getAttributes().getCaseType()));
        setRegion(defaultOptional(roleAssignment.getAttributes().getRegion()));
        setLocation(defaultOptional(roleAssignment.getAttributes().getLocation()));
        setCaseReference(defaultOptional(roleAssignment.getAttributes().getCaseId()));
        setSecurityClassification(roleAssignment.getClassification());
        setReadableCaseStates(readableCaseStates);
    }

    public boolean hasCaseReference() {
        return StringUtils.isNotBlank(caseReference);
    }

    public boolean hasReadableCaseStates() {
        return !CollectionUtils.isEmpty(readableCaseStates);
    }

    @SuppressWarnings("java:S2789")
    private String defaultOptional(Optional<String> optional) {
        return optional == null ? EMPTY : optional.orElse(EMPTY);
    }
}
