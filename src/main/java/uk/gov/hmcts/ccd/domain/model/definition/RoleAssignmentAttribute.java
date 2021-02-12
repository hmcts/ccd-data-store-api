package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RoleAssignmentAttribute {
    private String jurisdiction;
    private String caseId;
    private String region;
    private String location;
    private String contractType; // SALARIED, FEEPAY
}
