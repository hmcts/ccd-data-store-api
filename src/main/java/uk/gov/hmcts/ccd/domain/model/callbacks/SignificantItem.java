package uk.gov.hmcts.ccd.domain.model.callbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignificantItem {

    @JsonProperty("type")
    private SignificantItemType type;
    @JsonProperty("description")
    private String description;
    @JsonProperty("url")
    private String url;

    public SignificantItemType getType() {
        return type;
    }

    public void setType(SignificantItemType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
