package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public class JurisdictionDisplayProperties {
    private String id;
    private String name;
    private String description;

    private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();

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
    public List<CaseTypeDefinition> getCaseTypeDefinitions() {
        return caseTypeDefinitions;
    }

    public void setCaseTypeDefinitions(List<CaseTypeDefinition> caseTypeDefinitions) {
        this.caseTypeDefinitions = caseTypeDefinitions;
    }
}
