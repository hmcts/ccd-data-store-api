package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import java.util.List;

public class AccessProfile {

    private String caseTypeId;

    private Boolean readOnly;

    private String classification;

    private String roleName;

    private List<String> accessProfiles;

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getAccessProfiles() {
        return accessProfiles;
    }

    public void setAccessProfiles(List<String> accessProfiles) {
        this.accessProfiles = accessProfiles;
    }
}
