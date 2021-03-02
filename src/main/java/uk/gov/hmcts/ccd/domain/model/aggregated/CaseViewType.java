package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public class CaseViewType {
    private String id;
    private String name;
    private String description;
    private CaseViewJurisdiction jurisdiction;
    private boolean isPrintEnabled;

    public CaseViewType() {
        // default constructor
    }

    private CaseViewType(String id,
                         String name,
                         String description,
                         CaseViewJurisdiction jurisdiction,
                         boolean isPrintEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jurisdiction = jurisdiction;
        this.isPrintEnabled = isPrintEnabled;
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

    public boolean isPrintEnabled() {
        return isPrintEnabled;
    }

    public void setPrintEnabled(boolean printEnabled) {
        this.isPrintEnabled = printEnabled;
    }

    public static CaseViewType createFrom(CaseTypeDefinition caseTypeDefinition) {
        return new CaseViewType(caseTypeDefinition.getId(),
                                caseTypeDefinition.getName(),
                                caseTypeDefinition.getDescription(),
                                CaseViewJurisdiction.createFrom(caseTypeDefinition.getJurisdictionDefinition()),
                                caseTypeDefinition.getPrintableDocumentsUrl() != null);
    }
}
