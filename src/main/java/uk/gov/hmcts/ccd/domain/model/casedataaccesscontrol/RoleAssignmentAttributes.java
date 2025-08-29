package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAttributes {
    public static final String ATTRIBUTE_NOT_DEFINED = "Attribute not defined";
    private Optional<String> jurisdiction;
    private Optional<String> caseId;
    private Optional<String> caseType;
    private Optional<String> region;
    private Optional<String> location;
    private Optional<String> contractType;
    private Optional<String> caseAccessGroupId;
}
