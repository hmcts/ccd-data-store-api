package uk.gov.hmcts.ccd.migration;

import java.util.List;

public class CaseData {

    String caseReference;
    List<String> organisationId;

    public CaseData(String caseReference, List<String> organisationId) {
        this.caseReference = caseReference;
        this.organisationId = organisationId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public List<String> getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(List<String> organisationId) {
        this.organisationId = organisationId;
    }

    @Override
    public String toString() {
        return "CaseData{" +
            "caseReference='" + caseReference + '\'' +
            ", organisationId=" + organisationId +
            '}';
    }
}
