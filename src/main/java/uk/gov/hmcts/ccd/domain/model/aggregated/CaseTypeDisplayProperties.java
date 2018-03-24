package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseState;

public class CaseTypeDisplayProperties {
    private String id;
    private String name;
    private String description;
    private CaseState[] states;

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

    public CaseState[] getStates() {
        return states;
    }

    public void setStates(CaseState[] states) {
        this.states = states;
    }
}
