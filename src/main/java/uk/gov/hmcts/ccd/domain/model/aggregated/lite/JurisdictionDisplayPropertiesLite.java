package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class JurisdictionDisplayPropertiesLite {
    @Setter
    @Getter
    private String id;
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private String description;

    private List<CaseTypeLite> caseTypeLiteDefinitons = new ArrayList<>();

    @JsonProperty("caseTypes")
    public List<CaseTypeLite> getCaseTypeDefinitions() {
        return caseTypeLiteDefinitons;
    }

    public void setCaseTypeDefinitions(List<CaseTypeLite> caseTypeDefinitions) {
        this.caseTypeLiteDefinitons = caseTypeDefinitions;
    }
}
