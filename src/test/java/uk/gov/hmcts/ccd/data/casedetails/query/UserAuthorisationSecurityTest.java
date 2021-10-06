package uk.gov.hmcts.ccd.data.casedetails.query;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserAuthorisationSecurity")
class UserAuthorisationSecurityTest {

    private final List<String> caseReferences = Arrays.asList(new String[]{"5353535"});
    private static final String USER_ID = "123";
    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private CaseDetailsQueryBuilder<CaseDetailsEntity> builder;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @InjectMocks
    private UserAuthorisationSecurity security;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(userAuthorisation.getUserId()).thenReturn(USER_ID);
    }

    @Nested
    @DisplayName("secure()")
    class Secure {

        @Test
        @DisplayName("should not secure when user access level is ALL")
        void accessLevelAll() {
            when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.ALL);

            security.secure(builder, null);

            assertAll(
                () -> verify(builder, never()).whereGrantedAccessOnly(anyString())
            );
        }

        @Test
        @DisplayName("should secure for user ID when user access level is GRANTED")
        void accessLevelGranted() {
            doReturn(false).when(applicationParams).getEnableAttributeBasedAccessControl();
            when(userAuthorisation.getAccessLevel()).thenReturn(AccessLevel.GRANTED);
            security.secure(builder, null);

            assertAll(
                () -> verify(builder).whereGrantedAccessOnly(USER_ID)
            );
        }
    }

}
