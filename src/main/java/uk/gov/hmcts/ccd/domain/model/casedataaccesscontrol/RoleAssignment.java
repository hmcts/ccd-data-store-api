package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Builder
@Data
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
    private Instant beginTime;
    private Instant endTime;
    private Instant created;
    private List<String> authorisations;
    private RoleAssignmentAttributes attributes;

    /**
     * This is how the de-serialization works:
     * Missing caseId -> caseId == null
     * caseId: null -> Optional.empty()
     * caseId: "null" -> Optional.empty()
     * caseId: "12345" -> Optional.of("12345")
     */
    public boolean isCaseRoleAssignment() {
        return this.getAttributes() != null
            && this.getAttributes().getCaseId() != null
            && StringUtils.isNotBlank(this.getAttributes().getCaseId().orElse(""));
    }
}
