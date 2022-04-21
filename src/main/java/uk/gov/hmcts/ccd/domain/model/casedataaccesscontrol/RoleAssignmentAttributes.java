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
    public static final String ATTRIBUTE_NOT_DEFINED = "Attribute not defined";
    private String jurisdiction;
    private String caseId;
    private String caseType;
    private String region;
    private String location;
    private String contractType;
}
