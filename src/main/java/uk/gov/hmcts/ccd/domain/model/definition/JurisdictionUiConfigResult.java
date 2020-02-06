package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class JurisdictionUiConfigResult implements Serializable {

	private List<JurisdictionUiConfig> configs;

	public JurisdictionUiConfigResult() { }

	public JurisdictionUiConfigResult(List<JurisdictionUiConfig> configs) {
		this.configs = configs;
	}

	/**
	 **/
	@ApiModelProperty(value = "")
	@JsonProperty("configs")
	public List<JurisdictionUiConfig> getConfigs() {
		return configs;
	}

	public void setConfigs(List<JurisdictionUiConfig> configs) {
		this.configs = configs;
	}

}
