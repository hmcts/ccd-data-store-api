package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RoleToAccessProfileDefinition implements Serializable {
    private static final long serialVersionUID = 8882065812393433800L;

    private static final String AUTHORISATION_SEPARATOR = ",";
    @JsonProperty("case_type_id")
    private String caseTypeId;
    private Boolean disabled;
    @JsonProperty("read_only")
    private Boolean readOnly;
    private String authorisations;
    @JsonProperty("access_profiles")
    private String accessProfiles;
    @JsonProperty("live_from")
    private String liveFrom;
    @JsonProperty("live_to")
    private String liveTo;
    @JsonProperty("role_name")
    private String roleName;

    public RoleToAccessProfileDefinition(){

    }

    public RoleToAccessProfileDefinition(String caseTypeId, Boolean disabled, Boolean readOnly, String authorisations,
                                         String accessProfiles, String liveFrom, String liveTo, String roleName) {
        this.caseTypeId = caseTypeId;
        this.disabled = disabled;
        this.readOnly = readOnly;
        this.authorisations = authorisations;
        this.accessProfiles = accessProfiles;
        this.liveFrom = liveFrom;
        this.liveTo = liveTo;
        this.roleName = roleName;
    }

    public List<String> getAuthorisationList() {
        if (getAuthorisations() != null) {
            return Arrays.stream(getAuthorisations().split(AUTHORISATION_SEPARATOR))
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<String> getAccessProfileList() {
        if (getAccessProfiles() != null) {
            return Arrays.stream(getAccessProfiles().split(AUTHORISATION_SEPARATOR))
                .filter(str -> str.length() > 0)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public static String getAuthorisationSeparator() {
        return AUTHORISATION_SEPARATOR;
    }

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

    public void setAuthorisations(String authorisations) {
        this.authorisations = authorisations;
    }

    public String getAccessProfiles() {
        return accessProfiles;
    }

    public void setAccessProfiles(String accessProfiles) {
        this.accessProfiles = accessProfiles;
    }


    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(String liveFrom) {
        this.liveFrom = liveFrom;
    }

    public String getLiveTo() {
        return liveTo;
    }

    public void setLiveTo(String liveTo) {
        this.liveTo = liveTo;
    }
}
