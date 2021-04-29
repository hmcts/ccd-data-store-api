package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessProfile {

    private Boolean readOnly;
    private String classification;
    private String accessProfile;

    public AccessProfile(String accessProfile) {
        this.accessProfile = accessProfile;
    }
}
