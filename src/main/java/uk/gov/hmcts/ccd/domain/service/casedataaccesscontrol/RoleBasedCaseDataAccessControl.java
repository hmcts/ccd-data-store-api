package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Delete this class after we migrate CaseDataAccessControl fully.
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "false", matchIfMissing = true)
public class RoleBasedCaseDataAccessControl implements CaseDataAccessControl, AccessControl {
    private final UserAuthorisation userAuthorisation;
    private final CaseUserRepository caseUserRepository;

    @Autowired
    public RoleBasedCaseDataAccessControl(final UserAuthorisation userAuthorisation,
                                          final @Qualifier(CachedCaseUserRepository.QUALIFIER)
                                              CaseUserRepository caseUserRepository) {
        this.userAuthorisation = userAuthorisation;
        this.caseUserRepository = caseUserRepository;
    }

    @Override
    public void grantAccess(String caseId, String idamUserId) {
        if (UserAuthorisation.AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            caseUserRepository.grantAccess(Long.valueOf(caseId), idamUserId, CREATOR.getRole());
        }
    }

    @Override
    public List<AccessProfile> generateAccessProfiles(CaseTypeDefinition caseTypeDefinition) {
        throw new UnsupportedOperationException("Not used. Only here to comply with the interface.");
    }
}
