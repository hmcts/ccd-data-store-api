package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProfileCaseState {
    private String id;
    private String name;
    private String description;
    @JsonProperty("title_display")
    private String titleDisplay;

    public ProfileCaseState() {
        // default constructor
    }

    public ProfileCaseState(String id, String name, String description, String titleDisplay) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.titleDisplay = titleDisplay;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitleDisplay() {
        return titleDisplay;
    }

    public void setTitleDisplay(String titleDisplay) {
        this.titleDisplay = titleDisplay;
    }
}
