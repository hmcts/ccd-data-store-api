package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@ApiModel
@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
public class CaseStateDefinition implements Serializable {

    @JsonIgnore
    public static final String ANY = "*";

    String id;
    String name;
    String description;
    Integer displayOrder;
    String titleDisplay;
    @SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
    private List<AccessControlList> accessControlLists;

    @ApiModelProperty(required = true)
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * Short name to display.
     **/
    @ApiModelProperty(value = "Short name to display.")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    @ApiModelProperty(value = "Title label to be displayed for state")
    @JsonProperty("title_display")
    public String getTitleDisplay() {
        return titleDisplay;
    }

    @ApiModelProperty(value = "State Access Control Lists")
    @JsonProperty("acls")
    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }
}
