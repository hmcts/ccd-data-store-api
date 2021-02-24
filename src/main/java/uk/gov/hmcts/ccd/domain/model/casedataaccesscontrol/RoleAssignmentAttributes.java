package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RoleAssignmentAttributes {
    private String jurisdiction;
    private String caseId;
    private String region;
    private String location;
    private String contractType; // SALARIED, FEEPAY
}
