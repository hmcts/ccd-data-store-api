package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

public class CaseViewType {
    private String id;
    private String name;
    private String description;
    private CaseViewJurisdiction jurisdiction;
    private String printableDocumenstUrl;

    public CaseViewType() {
        // default constructor
    }

    private CaseViewType(String id, String name, String description, CaseViewJurisdiction jurisdiction, String printableDocumentsUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jurisdiction = jurisdiction;
        this.printableDocumenstUrl = printableDocumentsUrl;
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

    public String getPrintableDocumentsUrl() { return printableDocumenstUrl; }

    public void setPrintableDocumentsUrl(String printableDocumentUrl) { this.printableDocumenstUrl = printableDocumentUrl; }

    public static CaseViewType createFrom(CaseType caseType) {
        return new CaseViewType(caseType.getId(),
                                caseType.getName(),
                                caseType.getDescription(),
                                CaseViewJurisdiction.createFrom(caseType.getJurisdiction()),
                                caseType.getPrintableDocumentsUrl());
    }
}
