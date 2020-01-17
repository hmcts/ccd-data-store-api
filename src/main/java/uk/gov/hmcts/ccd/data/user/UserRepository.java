package uk.gov.hmcts.ccd.data.user;

import java.util.Set;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;

public interface UserRepository {

    IDAMProperties getUserDetails();

    IdamUser getUser();

    UserDefault getUserDefaultSettings(String userId);

    Set<String> getUserRoles();

    Set<SecurityClassification> getUserClassifications(String jurisdictionId);

    SecurityClassification getHighestUserClassification(String jurisdictionId);

    String getUserId();
}
