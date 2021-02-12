package uk.gov.hmcts.ccd.data.roleassignment;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RoleAssignmentRecordAttribute {
    String jurisdiction;
    String caseId;
    String region;
    String location;
    String contractType;
}
