package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class JurisdictionLiteDisplayProperties {
    private String id;
    private String name;
    private String description;

    private List<CaseTypeLiteDefinition> caseTypeLiteDefinitons = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("caseTypes")
    public List<CaseTypeLiteDefinition> getCaseTypeDefinitions() {
        return caseTypeLiteDefinitons;
    }

    public void setCaseTypeDefinitions(List<CaseTypeLiteDefinition> caseTypeDefinitions) {
        this.caseTypeLiteDefinitons = caseTypeDefinitions;
    }
}
