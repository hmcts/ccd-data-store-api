package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class MatchingResults {
    boolean validDate;
    boolean validCaseId;
    boolean validJurisdiction;
    boolean validRegion;
    boolean validLocation;
    boolean validClassification;
}
