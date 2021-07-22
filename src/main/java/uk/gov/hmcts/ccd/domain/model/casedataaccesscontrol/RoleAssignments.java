package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Builder
@Data
@AllArgsConstructor
public class RoleAssignments {
    private List<RoleAssignment> roleAssignments;

    private static final String ORGANISATION = "Organisation";
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public RoleAssignments(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                               CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @JsonIgnore
    public List<String> getJurisdictions() {
        List<String> jurisdiction = new ArrayList<>();
        roleAssignments.stream()
            .filter(roleAssignment -> !roleAssignment.getGrantType().contentEquals(GrantType.BASIC.name()))
            .collect(Collectors.toList());

        roleAssignments.forEach(roleAssignmentAttributes -> {
            if (roleAssignmentAttributes.getAttributes().getJurisdiction().isPresent()) {
                jurisdiction.add(roleAssignmentAttributes.getAttributes().getJurisdiction().get());
            } else if (roleAssignmentAttributes.getAttributes().getCaseType().isPresent()) {
                jurisdiction.add(caseDefinitionRepository
                    .getCaseType(roleAssignmentAttributes.getAttributes().getCaseType().get()).getJurisdictionId());
            } else if (roleAssignmentAttributes.getRoleType().contentEquals(ORGANISATION)) {
                caseDefinitionRepository.getAllJurisdiction(Optional.ofNullable(null))
                    .forEach(jurisdictionDefinition -> jurisdiction.add(jurisdictionDefinition.getId()));
            }
        });

        return jurisdiction;
    }
}
