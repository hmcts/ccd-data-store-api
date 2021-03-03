package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.Date;

public class RoleToAccessProfileDefinition {

    private String caseTypeId;
    private Boolean disabled;
    private Boolean readOnly;
    private String authorisation;
    private String accessProfiles;
    private Date liveFrom;
    private Date liveTo;
    private String roleName;

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getAuthorisation() {
        return authorisation;
    }

    public void setAuthorisation(String authorisation) {
        this.authorisation = authorisation;
    }

    public String getAccessProfiles() {
        return accessProfiles;
    }

    public void setAccessProfiles(String accessProfiles) {
        this.accessProfiles = accessProfiles;
    }

    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    public Date getLiveTo() {
        return liveTo;
    }

    public void setLiveTo(Date liveTo) {
        this.liveTo = liveTo;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
