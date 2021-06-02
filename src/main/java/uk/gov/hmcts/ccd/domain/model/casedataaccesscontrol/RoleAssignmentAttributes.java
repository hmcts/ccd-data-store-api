package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAttributes {
    private Optional<String> jurisdiction;
    private Optional<String> caseId;
    private Optional<String> caseTypeId;
    private Optional<String> region;
    private Optional<String> location;
    private Optional<String> contractType;
}
