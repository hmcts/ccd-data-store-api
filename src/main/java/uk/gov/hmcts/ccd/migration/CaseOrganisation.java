package uk.gov.hmcts.ccd.migration;

public class CaseOrganisation {

    String caseReference;
    String organisationId;

    public CaseOrganisation(String caseReference, String organisationId) {
        this.caseReference = caseReference;
        this.organisationId = organisationId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    @Override
    public String toString() {
        return "CaseOrganisation{" +
            "caseReference='" + caseReference + '\'' +
            ", organisationId='" + organisationId + '\'' +
            '}';
    }
}
