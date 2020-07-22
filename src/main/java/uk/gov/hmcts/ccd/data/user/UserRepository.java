package uk.gov.hmcts.ccd.data.user;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;

public interface UserRepository {

    IdamProperties getUserDetails();

    IdamUser getUser();

    UserDefault getUserDefaultSettings(String userId);

    Set<String> getUserRoles();

    Set<SecurityClassification> getUserClassifications(String jurisdictionId);

    SecurityClassification getHighestUserClassification(String jurisdictionId);

    String getUserId();

    /**
     * Get a user's jurisdictions based on their roles.
     * Note that for cross-jurisdictional roles, this method will NOT return every jurisdiction - it only takes into account
     * jurisdiction-specific roles.
     * If a user is cross-jurisdictional, the jurisdictions should be obtained from the CaseDefinitionRepository.
     * @return The jurisdictions the user has access to.
     */
    List<String> getUserRolesJurisdictions();

    boolean anyRoleEqualsAnyOf(List<String> userRoles);

    boolean anyRoleEqualsTo(String userRole);

    boolean anyRoleMatches(Pattern rolesPattern);
}
