package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings("checkstyle:SummaryJavadoc")
@ApiModel(description = "")
public class JurisdictionUiConfigResult implements Serializable {

    private List<JurisdictionUiConfigDefinition> configs;

    public JurisdictionUiConfigResult() {
    }

    public JurisdictionUiConfigResult(List<JurisdictionUiConfigDefinition> configs) {
        this.configs = configs;
    }

    /**
     *
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("configs")
    public List<JurisdictionUiConfigDefinition> getConfigs() {
        return configs;
    }

    public void setConfigs(List<JurisdictionUiConfigDefinition> configs) {
        this.configs = configs;
    }

}
