package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.CITIZEN;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.ENFORCEMENT;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.JUDICIAL;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.LEGAL_OPERATIONS;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.PROFESSIONAL;

@DisplayName("RoleAssignmentCategoryService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentCategoryServiceTest {

    private static final String USER_ID = "12345";

    @Mock
    private IdamRepository idamRepository;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private RoleAssignmentsMapper roleAssignmentsMapper;

    @InjectMocks
    private RoleAssignmentCategoryService roleAssignmentCategoryService;


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
        void shouldGetRoleCategoryForLegalOperationsUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("caseworker"));

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(LEGAL_OPERATIONS));
        }

        @Test
        void shouldGetRoleCategoryForEnforcementUser() {

            given(idamRepository.getUserRoles(USER_ID))
                .willReturn(singletonList("some-user"));

            RoleAssignment enforcementRole = RoleAssignment.builder()
                .roleName("bailiff-manager")
                .grantType(STANDARD.name())
                .build();

            given(roleAssignmentRepository.getRoleAssignments(USER_ID))
                .willReturn(new RoleAssignmentResponse());
            given(roleAssignmentsMapper.toRoleAssignments(any(RoleAssignmentResponse.class)))
                .willReturn(RoleAssignments.builder()
                    .roleAssignments(singletonList(enforcementRole))
                    .build());

            RoleCategory roleCategory = roleAssignmentCategoryService.getRoleCategory(USER_ID);

            assertThat(roleCategory, is(ENFORCEMENT));
        }

    }

}
