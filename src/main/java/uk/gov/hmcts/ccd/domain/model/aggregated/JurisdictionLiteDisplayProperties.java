package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class JurisdictionLiteDisplayProperties {
    @Setter
    @Getter
    private String id;
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private String description;

    private List<CaseTypeLiteDefinition> caseTypeLiteDefinitons = new ArrayList<>();

    @JsonProperty("caseTypes")
    public List<CaseTypeLiteDefinition> getCaseTypeDefinitions() {
        return caseTypeLiteDefinitons;
    }

    public void setCaseTypeDefinitions(List<CaseTypeLiteDefinition> caseTypeDefinitions) {
        this.caseTypeLiteDefinitons = caseTypeDefinitions;
    }
}
