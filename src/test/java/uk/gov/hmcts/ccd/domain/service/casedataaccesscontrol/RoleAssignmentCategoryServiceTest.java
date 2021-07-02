package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_CITIZEN;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_JUDICIAL;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_PROFESSIONAL;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_STAFF;

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
        public void shouldGetRoleCategoryForSolicitorUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("caseworker", "caseworker-autotest1-solicitor"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_PROFESSIONAL));
        }

        @Test
        public void shouldGetRoleCategoryForLocalAuthorityUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("caseworker", "caseworker-autotest1-localAuthority"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_PROFESSIONAL));
        }

        @Test
        public void shouldGetRoleCategoryForCitizenUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("citizen"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_CITIZEN));
        }

        @Test
        public void shouldGetRoleCategoryForLetterHolderUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("letter-holder"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_CITIZEN));
        }

        @Test
        public void shouldGetRoleCategoryForPanelMemberUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("judge1-panelmember"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_JUDICIAL));
        }

        @Test
        public void shouldGetRoleCategoryForStaffUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(Arrays.asList("caseworker"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ROLE_CATEGORY_STAFF));
        }
    }
}
