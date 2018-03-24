package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

public interface GetUserProfileOperation {
    UserProfile execute(String userToken);
}
