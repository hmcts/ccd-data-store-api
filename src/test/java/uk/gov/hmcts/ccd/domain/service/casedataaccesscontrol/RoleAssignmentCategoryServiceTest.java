package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.CITIZEN;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.JUDICIAL;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.LEGAL_OPERATIONS;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.PROFESSIONAL;

@DisplayName("RoleAssignmentCategoryService")
class RoleAssignmentCategoryServiceTest {
    private static final String USER_ID = "12345";

    @Mock
    private IdamRepository idamRepository;

    private RoleAssignmentCategoryService roleAssignmentCategoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        roleAssignmentCategoryService = new RoleAssignmentCategoryService(idamRepository);
    }

    @Nested
    @DisplayName("getRoleCategory()")
    class GetRoleCategory {

        @Test
        void shouldGetRoleCategoryForSolicitorUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(asList("caseworker", "caseworker-autotest1-solicitor"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(PROFESSIONAL));
        }

        @Test
        void shouldGetRoleCategoryForLocalAuthorityUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(asList("caseworker", "caseworker-autotest1-localAuthority"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(PROFESSIONAL));
        }

        @Test
        void shouldGetRoleCategoryForCitizenUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("citizen"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(CITIZEN));
        }

        @Test
        void shouldGetRoleCategoryForLetterHolderUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("letter-holder"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(CITIZEN));
        }

        @Test
        void shouldGetRoleCategoryForPanelMemberUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("judge1-panelmember"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(JUDICIAL));
        }

        @Test
        void shouldGetRoleCategoryForStaffUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("caseworker"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(LEGAL_OPERATIONS));
        }
    }
}
