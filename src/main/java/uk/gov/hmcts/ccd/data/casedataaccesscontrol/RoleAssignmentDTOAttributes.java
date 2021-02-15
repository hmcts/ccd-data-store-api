package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RoleAssignmentDTOAttributes {
    String jurisdiction;
    String caseId;
    String region;
    String location;
    String contractType;
}
