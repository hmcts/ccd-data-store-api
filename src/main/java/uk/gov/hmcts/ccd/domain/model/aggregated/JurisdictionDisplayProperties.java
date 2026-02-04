package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class JurisdictionDisplayProperties {
    private String id;
    private String name;
    private String description;
    @JsonProperty("caseTypes")
    private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();
}
