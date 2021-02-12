package uk.gov.hmcts.ccd.data.roleassignment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RoleAssignmentRecord {
    private String id;
    private String actorIdType;
    private String actorId;
    private String roleType;
    private String roleName;
    private String classification;
    private String grantType;
    private String roleCategory;
    private Boolean readOnly;
    private String beginTime;
    private String endTime;
    private String created;
    private List<String> authorisations;
    private List<RoleAssignmentRecordAttribute> attributes;
}
