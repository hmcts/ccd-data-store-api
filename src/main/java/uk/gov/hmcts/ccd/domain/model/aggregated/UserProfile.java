package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.aggregated.lite.JurisdictionDisplayPropertiesLite;

@Setter
@Getter
@ToString
public class UserProfile {

    private User user = new User();
    private String[] channels;
    private JurisdictionDisplayProperties[] jurisdictions;
    @JsonIgnore
    private JurisdictionDisplayPropertiesLite[] liteJurisdictions;
    @JsonProperty("default")
    private DefaultSettings defaultSettings = new DefaultSettings();
}
