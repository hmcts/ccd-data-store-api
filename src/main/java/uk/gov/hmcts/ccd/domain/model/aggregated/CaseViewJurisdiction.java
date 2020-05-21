package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;

public class CaseViewJurisdiction {
    private String id;
    private String name;
    private String description;

    public CaseViewJurisdiction() {
        // default constructor
    }

    private CaseViewJurisdiction(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

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

    public static CaseViewJurisdiction createFrom(JurisdictionDefinition jurisdictionDefinition) {
        return new CaseViewJurisdiction(jurisdictionDefinition.getId(),
            jurisdictionDefinition.getName(),
            jurisdictionDefinition.getDescription());
    }
}
