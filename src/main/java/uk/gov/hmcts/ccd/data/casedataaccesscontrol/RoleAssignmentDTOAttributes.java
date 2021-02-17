package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentDTOAttributes implements Serializable {
    private static final long serialVersionUID = -7106266789404292869L;

    String jurisdiction;
    String caseId;
    String region;
    String location;
    String contractType;
}
