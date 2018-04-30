package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@ToString
public class UserProfile {

    private User user = new User();
    private String[] channels;
    private JurisdictionDisplayProperties[] jurisdictions;
    @JsonProperty("default")
    private DefaultSettings defaultSettings = new DefaultSettings();

    public String[] getChannels() {
        return channels;
    }

    public void setChannels(String[] channels) {
        this.channels = channels;
    }

    public JurisdictionDisplayProperties[] getJurisdictions() {
        return jurisdictions;
    }

    public void setJurisdictions(JurisdictionDisplayProperties[] jurisdictions) {
        this.jurisdictions = jurisdictions;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DefaultSettings getDefaultSettings() {
        return defaultSettings;
    }

    public void setDefaultSettings(DefaultSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

}
