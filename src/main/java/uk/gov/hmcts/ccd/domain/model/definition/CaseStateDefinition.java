package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema
public class CaseStateDefinition implements Serializable, Copyable<CaseStateDefinition> {

    @JsonIgnore
    public static final String ANY = "*";

    private String id = null;
    private String name = null;
    private String description = null;
    private Integer displayOrder = null;
    private String titleDisplay;
    private List<AccessControlList> accessControlLists;
    private String stateCategory = null;

    @Schema(required = true)
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Short name to display.
     **/
    @Schema(description = "Short name to display.")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Schema(description = "Title label to be displayed for state")
    @JsonProperty("title_display")
    public String getTitleDisplay() {
        return titleDisplay;
    }

    public void setTitleDisplay(String titleDisplay) {
        this.titleDisplay = titleDisplay;
    }

    @Schema(description = "State Access Control Lists")
    @JsonProperty("acls")
    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }

    @JsonIgnore
    public String getStateCategory() {
        return stateCategory;
    }

    @JsonProperty("state_category")
    public void setStateCategory(String stateCategory) {
        this.stateCategory = stateCategory;
    }

    @JsonIgnore
    @Override
    public CaseStateDefinition createCopy() {
        CaseStateDefinition copy = new CaseStateDefinition();
        copy.setId(this.getId());
        copy.setName(this.getName());
        copy.setDescription(this.getDescription());
        copy.setDisplayOrder(this.getDisplayOrder());
        copy.setTitleDisplay(this.getTitleDisplay());
        copy.setAccessControlLists(createACLCopyList(this.getAccessControlLists()));
        copy.setStateCategory(this.getStateCategory());

        return copy;
    }
}
