package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.ToString;

@ApiModel(description = "")
@ToString
public class JurisdictionDefinition implements Serializable, Copyable<JurisdictionDefinition> {

    private String id = null;
    private String name = null;
    private String description = null;
    private Date liveFrom = null;
    private Date liveUntil = null;

    private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();

    @ApiModelProperty(required = true, value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(required = true, value = "")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(Date liveUntil) {
        this.liveUntil = liveUntil;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("case_types")
    public List<CaseTypeDefinition> getCaseTypeDefinitions() {
        return caseTypeDefinitions;
    }

    public void setCaseTypeDefinitions(List<CaseTypeDefinition> caseTypeDefinitions) {
        this.caseTypeDefinitions = caseTypeDefinitions;
    }

    public List<String> getCaseTypesIDs() {
        return this.getCaseTypeDefinitions().stream().map(CaseTypeDefinition::getId).collect(Collectors.toList());
    }

    @JsonIgnore
    @Override
    public JurisdictionDefinition createCopy() {
        JurisdictionDefinition copy = new JurisdictionDefinition();
        copy.setId(this.id);
        copy.setName(this.name);
        copy.setDescription(this.description);
        copy.setLiveFrom(this.liveFrom != null ? new Date(this.liveFrom.getTime()) : null);
        copy.setLiveUntil(this.liveUntil != null ? new Date(this.liveUntil.getTime()) : null);
        copy.setCaseTypeDefinitions(this.caseTypeDefinitions != null
            ? createCopyList(this.caseTypeDefinitions) : null);

        return copy;
    }
}
