package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema
public class UserDefaultCollection {

    private List<UserDefault> userDefaults;

    public UserDefaultCollection() {
    }

    public UserDefaultCollection(List<UserDefault> userDefaults) {
        this.userDefaults = userDefaults;
    }

    @Schema
    @JsonProperty("user_profiles")
    public List<UserDefault> getUserDefaults() {
        return userDefaults;
    }

    public void setUserDefaults(List<UserDefault> userDefaults) {
        this.userDefaults = userDefaults;
    }
}
