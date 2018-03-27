package uk.gov.hmcts.ccd.data.user;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;

public interface UserRepository {

    IDAMProperties getUserDetails();

    CompletableFuture<IDAMProperties> getUserDetailsAsync();

    CompletableFuture<UserDefault> getUserDefaultSettingsAsync(String userId);

    Set<String> getUserRoles();

    Set<SecurityClassification> getUserClassifications(String jurisdictionId);
}
