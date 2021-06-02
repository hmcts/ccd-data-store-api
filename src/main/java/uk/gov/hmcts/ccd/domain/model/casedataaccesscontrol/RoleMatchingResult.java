package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class RoleMatchingResult {
    boolean dateMatched;
    boolean caseIdMatched;
    boolean caseTypeIdMatched;
    boolean jurisdictionMatched;
    boolean regionMatched;
    boolean locationMatched;
    boolean classificationMatched;

    public boolean matchedAllValues() {
        return isDateMatched()
            && isCaseIdMatched()
            && isCaseTypeIdMatched()
            && isJurisdictionMatched()
            && isClassificationMatched()
            && isRegionMatched()
            && isLocationMatched();
    }
}
