package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;


@SuppressWarnings("checkstyle:SummaryJavadoc")
@Schema
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
    @Schema
    @JsonProperty("configs")
    public List<JurisdictionUiConfigDefinition> getConfigs() {
        return configs;
    }

    public void setConfigs(List<JurisdictionUiConfigDefinition> configs) {
        this.configs = configs;
    }

}
