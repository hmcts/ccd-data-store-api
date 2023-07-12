package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@ApiModel
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class JurisdictionDefinition implements Serializable {

    String id;
    String name;
    String description;
    Date liveFrom;
    Date liveUntil;

    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    @Builder.Default
    private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();

    @ApiModelProperty(required = true)
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @ApiModelProperty(required = true)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @ApiModelProperty
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    @ApiModelProperty
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    @ApiModelProperty
    @JsonProperty("case_types")
    public List<CaseTypeDefinition> getCaseTypeDefinitions() {
        return caseTypeDefinitions;
    }


    public List<String> getCaseTypesIDs() {
        return this.getCaseTypeDefinitions().stream().map(CaseTypeDefinition::getId).collect(Collectors.toList());
    }
}
