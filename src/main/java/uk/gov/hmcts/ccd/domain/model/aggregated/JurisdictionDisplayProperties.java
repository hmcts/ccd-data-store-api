package uk.gov.hmcts.ccd.domain.model.aggregated;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

public class JurisdictionDisplayProperties {
    private String id;
    private String name;
    private String description;

    private List<CaseType> caseTypes = new ArrayList<>();

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

    public List<CaseType> getCaseTypes() {
        return caseTypes;
    }

    public void setCaseTypes(List<CaseType> caseTypes) {
        this.caseTypes = caseTypes;
    }
}
