package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

class RoleBasedCaseDataAccessControlTest {

    private static final String IDAM_ID = "23";
    private static final String CASE_ID = "45677";

    @Mock
    private CaseUserRepository caseUserRepository;

    @Mock
    private UserAuthorisation userAuthorisation;

    @InjectMocks
    private RoleBasedCaseDataAccessControl instance;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("when creator has access level GRANTED, then it should grant access to creator")
    void shouldGrantAccessToAccessLevelGrantedCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.GRANTED);

        instance.grantAccess(CASE_ID, IDAM_ID);

        verify(caseUserRepository).grantAccess(Long.valueOf(CASE_ID), IDAM_ID, CREATOR.getRole());
    }

    @Test
    @DisplayName("when creator has access level ALL, then it should NOT grant access to creator")
    void shouldNotGrantAccessToAccessLevelAllCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);

        instance.grantAccess(CASE_ID, IDAM_ID);

        verifyZeroInteractions(caseUserRepository);
    }
}
