package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class RoleAssignmentDTO {
    private String id;
    private String actorIdType;
    private String actorId;
    private String roleType;
    private String roleName;
    private String classification;
    private String grantType;
    private String roleCategory;
    private Boolean readOnly;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime created;
    private List<String> authorisations;
    private RoleAssignmentDTOAttributes attributes;
}
