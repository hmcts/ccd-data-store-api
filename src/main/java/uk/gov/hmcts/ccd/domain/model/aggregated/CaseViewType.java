package uk.gov.hmcts.ccd.domain.model.aggregated;

public class CaseViewType {
    private String id;
    private String name;
    private String description;
    private CaseViewJurisdiction jurisdiction;

    public CaseViewType() {
        // default constructor
    }

    public CaseViewType(String id, String name, String description, CaseViewJurisdiction jurisdiction) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jurisdiction = jurisdiction;
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

    public CaseViewJurisdiction getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(CaseViewJurisdiction jurisdiction) {
        this.jurisdiction = jurisdiction;
    }
}
