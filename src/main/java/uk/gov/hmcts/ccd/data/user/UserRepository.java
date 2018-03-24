package uk.gov.hmcts.ccd.data.user;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

import java.util.Set;

public interface UserRepository {
    UserProfile getUserSettings();

    IDAMProperties getUserDetails();

    Set<String> getUserRoles();

    Set<SecurityClassification> getUserClassifications(String jurisdictionId);
}
