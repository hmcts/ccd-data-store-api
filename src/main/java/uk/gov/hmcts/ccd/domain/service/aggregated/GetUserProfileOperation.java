package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.function.Predicate;

import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;

public interface GetUserProfileOperation {
    UserProfile execute(Predicate<AccessControlList> access);
}
