package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAttributes {
    private String jurisdiction;
    private String caseId;
    private String region;
    private String location;
    private String contractType; // SALARIED, FEEPAY
}
