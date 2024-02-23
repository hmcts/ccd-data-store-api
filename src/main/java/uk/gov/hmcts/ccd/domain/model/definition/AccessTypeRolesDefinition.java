package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AccessTypeRolesDefinition implements Serializable, Copyable<AccessTypeRolesDefinition> {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    @JsonProperty("live_from")
    private LocalDate liveFrom;

    @JsonProperty("live_to")
    private LocalDate liveTo;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    @JsonProperty("access_type_id")
    private String accessTypeId;

    @JsonProperty("organisation_profile_id")
    private String organisationProfileId;

    @JsonProperty("access_mandatory")
    private Boolean accessMandatory;

    @JsonProperty("access_default")
    private Boolean accessDefault;

    @JsonProperty("display")
    private Boolean display;

    @JsonProperty("description")
    private String description;

    @JsonProperty("hint")
    private String hint;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("organisational_role_name")
    private String organisationalRoleName;

    @JsonProperty("group_role_name")
    private String groupRoleName;

    @JsonProperty("case_assigned_role_field")
    private String caseAssignedRoleField;

    @JsonProperty("group_access_enabled")
    private Boolean groupAccessEnabled;

    @JsonProperty("case_access_group_id_template")
    private String caseAccessGroupIdTemplate;

    @JsonIgnore
    @Override
    public AccessTypeRolesDefinition createCopy() {
        return new AccessTypeRolesDefinition(
            this.id,
            this.liveFrom != null ? LocalDate.from(this.liveFrom) : null,
            this.liveTo != null ? LocalDate.from(this.liveTo) : null,
            this.caseTypeId,
            this.accessTypeId,
            this.organisationProfileId,
            this.accessMandatory,
            this.accessDefault,
            this.display,
            this.description,
            this.hint,
            this.displayOrder,
            this.organisationalRoleName,
            this.groupRoleName,
            this.caseAssignedRoleField,
            this.groupAccessEnabled,
            this.caseAccessGroupIdTemplate
        );
    }
}
