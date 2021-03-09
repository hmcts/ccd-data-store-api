package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

public class AccessProfile {

    private Boolean readOnly;
    private String classification;
    private String accessProfile;

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

    public String getAccessProfile() {
        return accessProfile;
    }

    public void setAccessProfile(String accessProfile) {
        this.accessProfile = accessProfile;
    }
}
