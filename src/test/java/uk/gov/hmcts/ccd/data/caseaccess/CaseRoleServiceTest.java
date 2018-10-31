package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;

class CaseRoleServiceTest {
    private final String caseId = "11223344";
    private final String userId = "26";
    private final List<String> caseRoles = Arrays.asList("[CASE_ROLE_1]", "[CASE_ROLE_2]");

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseUserRepository caseUserRepository;

    CaseRoleService classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(userId).when(userRepository).getUserId();
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(caseId), userId);


        classUnderTest = new CaseRoleService(userRepository, caseUserRepository);
    }

    @Test
    void getCaseRoles() {
        Set<String> caseRoles = classUnderTest.getCaseRoles(caseId);

        assertAll(
            () -> assertThat(caseRoles.size(), is(2)),
            () -> assertThat(caseRoles, hasItems("[CASE_ROLE_1]", "[CASE_ROLE_2]"))
        );
    }
}
