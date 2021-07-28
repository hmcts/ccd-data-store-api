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
import java.util.Objects;
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
        List<String> jurisdictions = new ArrayList<>();

        roleAssignments.stream()
            .filter(roleAssignment -> !roleAssignment.getGrantType().equals(GrantType.BASIC.name()))
            .collect(Collectors.toList())
            .forEach(roleAssignmentAttributes -> {
                if (roleAssignmentAttributes.getAttributes() != null) {
                    if (Objects.nonNull(roleAssignmentAttributes.getAttributes().getJurisdiction())) {
                        String jurisdiction = roleAssignmentAttributes
                            .getAttributes().getJurisdiction().orElse(null);
                        jurisdictions.add(jurisdiction);
                    } else if (Objects.nonNull(roleAssignmentAttributes.getAttributes().getCaseType())) {
                        jurisdictions.add(caseDefinitionRepository
                            .getCaseType(roleAssignmentAttributes
                                .getAttributes().getCaseType().get()).getJurisdictionId());
                    } else if (Objects.nonNull(roleAssignmentAttributes.getRoleType())
                        && roleAssignmentAttributes.getRoleType().contentEquals(ORGANISATION)) {
                        caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore(Optional.empty())
                            .forEach(jurisdictionDefinition -> jurisdictions.add(jurisdictionDefinition.getId()));
                    }
                }
            });
        return jurisdictions;
    }


}
