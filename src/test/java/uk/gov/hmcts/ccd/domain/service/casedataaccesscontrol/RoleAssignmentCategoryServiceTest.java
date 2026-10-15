package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.LEGAL_OPERATIONS;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

@DisplayName("RoleAssignmentCategoryService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentCategoryServiceTest {

    private static final String USER_ID = "12345";

    @Mock private IdamRepository idamRepository;

    @InjectMocks private RoleAssignmentCategoryService roleAssignmentCategoryService;

    @Nested
    @DisplayName("getRoleCategory()")
    class GetRoleCategory {

        @ParameterizedTest
        @CsvSource({
            "pui-case-manager, PROFESSIONAL",
            "solicitor, PROFESSIONAL",
            "caseworker-autotest1-solicitor, PROFESSIONAL",
            "caseworker-autotest1-localAuthority, PROFESSIONAL",
            "citizen, CITIZEN",
            "letter-holder, CITIZEN",
            "judge1-panelmember, JUDICIAL"
        })
        void shouldGetRoleCategoryForUser(String roleName, RoleCategory expectedRoleCategory) {
            given(idamRepository.getUserRoles(USER_ID)).willReturn(asList("caseworker", roleName));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(expectedRoleCategory));
        }

        @Test
        void shouldGetRoleCategoryForLegalOperationsUser() {

            given(idamRepository.getUserRoles(USER_ID)).willReturn(singletonList("caseworker"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(LEGAL_OPERATIONS));
        }
    }
}
