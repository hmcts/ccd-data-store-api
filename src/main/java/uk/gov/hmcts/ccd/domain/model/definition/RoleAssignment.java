package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleAssignment {
    private String id;
    private String actorIdType; // currently IDAM
    private String actorId;
    private String roleType; // ORGANISATION, CASE
    private String roleName;
    private String classification;
    private String grantType; // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
    private String roleCategory; // JUDICIAL, STAFF
    private Boolean readOnly;
    private String beginTime; //  "YYYY-MM-DDTHH:MI:SSZ"
    private String endTime; //  "YYYY-MM-DDTHH:MI:SSZ"
    private String created; //  "YYYY-MM-DDTHH:MI:SSZ"
    private List<String> authorisations;
    private List<RoleAssignmentAttribute> attributes;
}
