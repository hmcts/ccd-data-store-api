package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessProfile {

    private Boolean readOnly;
    private String securityClassification;
    private String accessProfile;
    private String caseAccessCategories;

    public AccessProfile(String accessProfile) {
        this.accessProfile = accessProfile;
    }
}
