package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class RoleMatchingResult {
    boolean dateMatched;
    boolean caseIdMatched;
    boolean jurisdictionMatched;
    boolean regionMatched;
    boolean locationMatched;
    boolean classificationMatched;

    public boolean matchedAllValues() {
        return isDateMatched()
            && isCaseIdMatched()
            && isJurisdictionMatched()
            && isClassificationMatched()
            && isRegionMatched()
            && isLocationMatched();
    }
}
