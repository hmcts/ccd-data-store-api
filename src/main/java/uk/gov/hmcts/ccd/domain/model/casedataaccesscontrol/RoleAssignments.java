package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignments {
    private List<RoleAssignment> roleAssignments;

    @JsonIgnore
    public List<String> getJurisdictions() {
        return roleAssignments.stream()
            .map(RoleAssignment::getAttributes)
            .filter(Objects::nonNull)
            .map(RoleAssignmentAttributes::getJurisdiction)
            .filter(Objects::nonNull)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

}
