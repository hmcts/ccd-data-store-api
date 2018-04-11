package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.concurrent.CompletableFuture;

import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;

public interface GetUserProfileOperation {
    CompletableFuture<UserProfile> execute(String userToken);
}
