package uk.gov.hmcts.ccd.domain.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Optional;

import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

/**
 * Delete this class after we migrate CaseDataAccessControl fully.
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "ccd.access-control", havingValue = "false", matchIfMissing = true)
public class LegacyCaseDataAccessControl implements CaseDataAccessControl, AccessControl {
    private final UserAuthorisation userAuthorisation;
    private final CaseUserRepository caseUserRepository;

    @Autowired
    public LegacyCaseDataAccessControl(final UserAuthorisation userAuthorisation,
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

    /**
     * Not used. Only here to comply with the interface.
     */
    @Override
    public Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails) {
        return Optional.empty();
    }
}
