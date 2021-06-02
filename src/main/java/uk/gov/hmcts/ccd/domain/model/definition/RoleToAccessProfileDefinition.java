package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleToAccessProfileDefinition implements Serializable {
    private static final long serialVersionUID = 8882065812393433800L;

    private static final String AUTHORISATION_SEPARATOR = ",";

    private String caseTypeId;
    private Boolean disabled;
    private Boolean readOnly;
    private String authorisations;
    private String accessProfiles;
    private Date liveFrom;
    private Date liveTo;
    private String roleName;

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
