package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel
public class UserDefaultCollection {

    private List<UserDefault> userDefaults;

    public UserDefaultCollection() {
    }

    public UserDefaultCollection(List<UserDefault> userDefaults) {
        this.userDefaults = userDefaults;
    }

    @ApiModelProperty
    @JsonProperty("user_profiles")
    public List<UserDefault> getUserDefaults() {
        return userDefaults;
    }

    public void setUserDefaults(List<UserDefault> userDefaults) {
        this.userDefaults = userDefaults;
    }
}
