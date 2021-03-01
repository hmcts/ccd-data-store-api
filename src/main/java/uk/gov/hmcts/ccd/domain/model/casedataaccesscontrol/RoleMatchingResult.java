package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class RoleMatchingResult {
    boolean validDate;
    boolean validCaseId;
    boolean validJurisdiction;
    boolean validRegion;
    boolean validLocation;
    boolean validClassification;

    public boolean matchedAllValues() {
        return isValidDate()
            && isValidCaseId()
            && isValidJurisdiction()
            && isValidClassification()
            && isValidRegion()
            && isValidLocation();
    }

    public boolean matchedAExceptRegionAndLocation() {
        return isValidDate()
            && isValidCaseId()
            && isValidJurisdiction()
            && isValidClassification();
    }
}
