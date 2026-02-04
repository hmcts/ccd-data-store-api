package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class JurisdictionDisplayPropertiesLite {

    private String id;
    private String name;
    private String description;
    @JsonProperty("caseTypes")
    private List<CaseTypeLite> caseTypeLiteDefinitions = new ArrayList<>();
}
