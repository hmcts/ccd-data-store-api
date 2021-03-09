package uk.gov.hmcts.ccd.domain.model.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RoleToAccessProfileDefinition {

    private String caseTypeId;
    private Boolean disabled;
    private Boolean readOnly;
    private String authorisations;
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

    public String getAuthorisations() {
        return authorisations;
    }

    public List<String> getAuthorisationList() {
        if (getAuthorisations() != null) {
            return Arrays.asList(getAuthorisations().split(","))
                .stream()
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public void setAuthorisations(String authorisations) {
        this.authorisations = authorisations;
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
