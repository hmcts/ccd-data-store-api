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

import javax.persistence.Column;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AccessTypeDefinition implements Serializable, Copyable<AccessTypeDefinition> {

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

    @Column(name = "access_mandatory")
    private Boolean accessMandatory;

    @Column(name = "access_default")
    private Boolean accessDefault;

    @Column(name = "display")
    private Boolean display;

    @Column(name = "description")
    private String description;

    @Column(name = "hint")
    private String hint;

    @Column(name = "display_order")
    private Integer displayOrder;

    @JsonIgnore
    @Override
    public AccessTypeDefinition createCopy() {
        return new AccessTypeDefinition(
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
            this.displayOrder
        );
    }
}
