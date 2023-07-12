package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class RoleToAccessProfileDefinition implements Serializable {
    static final long serialVersionUID = 8882065812393433800L;

    static final String AUTHORISATION_SEPARATOR = ",";
    @JsonProperty("case_type_id")
    String caseTypeId;
    @Builder.Default
    Boolean disabled = Boolean.FALSE;
    @JsonProperty("read_only")
    @Builder.Default
    Boolean readOnly = Boolean.FALSE;
    String authorisations;
    @JsonProperty("access_profiles")
    String accessProfiles;
    @JsonProperty("live_from")
    String liveFrom;
    @JsonProperty("live_to")
    String liveTo;
    @JsonProperty("role_name")
    String roleName;
    @JsonProperty("case_access_categories")
    String caseAccessCategories;

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
}
