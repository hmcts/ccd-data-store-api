package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleToAccessProfileDefinition implements Serializable, Copyable<RoleToAccessProfileDefinition> {
    private static final long serialVersionUID = 8882065812393433800L;

    private static final String AUTHORISATION_SEPARATOR = ",";
    @JsonProperty("case_type_id")
    private String caseTypeId;
    private Boolean disabled;
    @JsonProperty("read_only")
    private Boolean readOnly;
    private String authorisations;
    @JsonProperty("access_profiles")
    private String accessProfiles;
    @JsonProperty("live_from")
    private String liveFrom;
    @JsonProperty("live_to")
    private String liveTo;
    @JsonProperty("role_name")
    private String roleName;
    @JsonProperty("case_access_categories")
    private String caseAccessCategories;

    public List<String> getAuthorisationList() {
        if (getAuthorisations() != null) {
            return Arrays.stream(getAuthorisations().split(AUTHORISATION_SEPARATOR))
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<String> getAccessProfileList() {
        if (getAccessProfiles() != null) {
            return Arrays.stream(getAccessProfiles().split(AUTHORISATION_SEPARATOR))
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @JsonIgnore
    @Override
    public RoleToAccessProfileDefinition createCopy() {
        return RoleToAccessProfileDefinition.builder()
            .caseTypeId(this.caseTypeId)
            .disabled(this.disabled)
            .readOnly(this.readOnly)
            .authorisations(this.authorisations)
            .accessProfiles(this.accessProfiles)
            .liveFrom(this.liveFrom)
            .liveTo(this.liveTo)
            .roleName(this.roleName)
            .caseAccessCategories(this.caseAccessCategories)
            .build();
    }
}
