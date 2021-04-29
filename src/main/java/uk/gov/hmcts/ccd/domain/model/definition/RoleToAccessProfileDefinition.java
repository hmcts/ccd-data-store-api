package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleToAccessProfileDefinition {

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
            return Arrays.asList(getAuthorisations().split(","))
                .stream()
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<String> getAccessProfileList() {
        if (getAccessProfiles() != null) {
            return Arrays.asList(getAccessProfiles().split(","))
                .stream()
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
