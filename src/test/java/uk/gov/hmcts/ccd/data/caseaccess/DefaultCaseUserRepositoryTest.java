package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentCategoryService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@Transactional
public class DefaultCaseUserRepositoryTest extends WireMockBaseTest {

    private static final String COUNT_CASE_USERS =
        "select count(*) from case_users where case_data_id = ? and user_id = ? and case_role = ?";

    private static final Long CASE_ID = 1L;
    private static final Long CASE_ID_GRANTED = 2L;
    private static final Long CASE_ID_3 = 3L;
    private static final String USER_ID = "89000";
    private static final String USER_ID_GRANTED = "89001";
    private static final String USER_ID_CLAIMANT = "89002";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_SOLICITOR = "[SOLICITOR]";
    private static final String CASE_ROLE_CREATOR = "[CREATOR]";
    private static final String CASE_ROLE_CLAIMANT = "[CLAIMANT]";

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate template;

    @MockBean
    private CaseUserAuditRepository auditRepository;

    @MockBean
    RoleAssignmentCategoryService roleAssignmentCategoryService;

    @Autowired
    private DefaultCaseUserRepository repository;

    @Before
    public void setUp() {
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldGrantAccessAsCustomCaseRole() {
        doReturn(RoleCategory.CITIZEN).when(roleAssignmentCategoryService).getRoleCategory(any());

        repository.grantAccess(CASE_ID, USER_ID, CASE_ROLE);

        assertThat(countAccesses(CASE_ID, USER_ID, CASE_ROLE), equalTo(1));
        verify(auditRepository).auditGrant(CASE_ID, USER_ID, CASE_ROLE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldRevokeAccessAsCustomCaseRole() {
        repository.revokeAccess(CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE);

        assertThat(countAccesses(CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE), equalTo(0));
        verify(auditRepository).auditRevoke(CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldFindCasesUserIdHasAccessTo() {
        List<Long> caseIds = repository.findCasesUserIdHasAccessTo(USER_ID);

        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CASE_ID));

        caseIds = repository.findCasesUserIdHasAccessTo(USER_ID_GRANTED);

        assertThat(caseIds.size(), equalTo(3));
        assertThat(caseIds, containsInAnyOrder(CASE_ID_GRANTED, CASE_ID_GRANTED, CASE_ID_3));
    }

    private Integer countAccesses(Long caseId, String userId) {
        return countAccesses(caseId, userId, GlobalCaseRole.CREATOR.getRole());
    }

    private Integer countAccesses(Long caseId, String userId, String role) {
        em.flush();

        final Object[] parameters = new Object[]{
            caseId,
            userId,
            role
        };

        return template.queryForObject(COUNT_CASE_USERS, parameters, Integer.class);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldFindCaseRolesUserPerformsForCase() {

        List<String> caseRoles = repository.findCaseRoles(CASE_ID, USER_ID);

        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_CREATOR));

        caseRoles = repository.findCaseRoles(CASE_ID_GRANTED, USER_ID_GRANTED);

        assertThat(caseRoles.size(), equalTo(2));
        assertThat(caseRoles, containsInAnyOrder(CASE_ROLE, CASE_ROLE_SOLICITOR));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldFindCaseAssignedCaseUserRolesForCaseAndUserList() {
        List<CaseUserEntity> caseUserEn =
            repository.findCaseUserRoles(Lists.newArrayList(CASE_ID), Lists.newArrayList(USER_ID));

        assertThat(caseUserEn.size(), equalTo(1));
        assertThat(caseUserEn.get(0).getCasePrimaryKey().getCaseRole(), equalTo(CASE_ROLE_CREATOR));
        assertThat(caseUserEn.get(0).getRoleCategory(), equalTo(RoleCategory.CITIZEN.name()));

        caseUserEn = repository.findCaseUserRoles(Lists.newArrayList(CASE_ID_GRANTED), Lists.newArrayList());

        assertThat(caseUserEn.size(), equalTo(2));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldFindCaseUserRolesForAUserId() {
        Set<String> caseUsers = repository.getCaseUserRolesByUserId(USER_ID);

        assertThat(caseUsers.size(), equalTo(1));
        assertThat(caseUsers.stream().findFirst().get(), equalTo(CASE_ROLE_CREATOR));

        caseUsers = repository.getCaseUserRolesByUserId(USER_ID_GRANTED);

        assertThat(caseUsers.size(), equalTo(3));
        assertThat(caseUsers, hasItems(CASE_ROLE, CASE_ROLE_SOLICITOR, CASE_ROLE_CLAIMANT));

        caseUsers = repository.getCaseUserRolesByUserId(USER_ID_CLAIMANT);
        assertThat(caseUsers.size(), equalTo(1));
        assertThat(caseUsers.stream().findFirst().get(), equalTo(CASE_ROLE_CLAIMANT));

        caseUsers = repository.getCaseUserRolesByUserId("16");
        assertThat(caseUsers.size(), equalTo(0));
    }

}
