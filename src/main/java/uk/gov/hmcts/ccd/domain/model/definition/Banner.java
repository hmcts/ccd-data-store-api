package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import lombok.ToString;

@ToString
public class Banner implements Serializable {

    private String id;

    private Boolean bannerEnabled;

    private String bannerDescription;

    private String bannerUrlText;

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
