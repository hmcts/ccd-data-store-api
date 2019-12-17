package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.ToString;

@ToString
public class Banner implements Serializable {

    private String id;

    @JsonProperty("banner_enabled")
    private Boolean bannerEnabled;

    @JsonProperty("banner_description")
    private String bannerDescription;

    @JsonProperty("banner_url_text")
    private String bannerUrlText;

    @JsonProperty("banner_url")
    private String bannerUrl;

    private Jurisdiction jurisdiction;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getBannerEnabled() {
        return bannerEnabled;
    }

    public void setBannerEnabled(Boolean bannerEnabled) {
        this.bannerEnabled = bannerEnabled;
    }

    public String getBannerDescription() {
        return bannerDescription;
    }

    public void setBannerDescription(String bannerDescription) {
        this.bannerDescription = bannerDescription;
    }

    public String getBannerUrlText() {
        return bannerUrlText;
    }

    public void setBannerUrlText(String bannerUrlText) {
        this.bannerUrlText = bannerUrlText;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public Jurisdiction getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(Jurisdiction jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

}
