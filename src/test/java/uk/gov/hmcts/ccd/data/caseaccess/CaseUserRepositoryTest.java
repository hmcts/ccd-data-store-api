package uk.gov.hmcts.ccd.data.caseaccess;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.BaseTest;

@Transactional
public class CaseUserRepositoryTest extends BaseTest {

    public static final String COUNT_CASE_USERS = "select count(*) from case_users where case_data_id = ? and user_id = ? and case_role = ?";

    private static final Long CASE_ID = 1L;
    private static final String USER_ID = "89000";

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate template;

    @MockBean
    private CaseUserAuditRepository auditRepository;

    @Autowired
    private CaseUserRepository repository;

    @Before
    public void setUp() {
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldGrantAccessAsCreator() {
        repository.grantAccess(CASE_ID, USER_ID);

        assertThat(countAccesses(CASE_ID, USER_ID), equalTo(1));
        verify(auditRepository).auditGrant(CASE_ID, USER_ID);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldRevokeAccessAsCreator() {
        repository.revokeAccess(CASE_ID, USER_ID);

        assertThat(countAccesses(CASE_ID, USER_ID), equalTo(0));
        verify(auditRepository).auditRevoke(CASE_ID, USER_ID);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql",
    })
    public void shouldFindCasesUserIdHasAccessTo() {
        final List<Long> casesId = repository.findCasesUserIdHasAccessTo(USER_ID);

        assertThat(casesId.size(), equalTo(1));
        assertThat(casesId.get(0), equalTo(CASE_ID));
    }

    private Integer countAccesses(Long caseId, String userId) {
        em.flush();

        final Object[] parameters = new Object[]{
            caseId,
            userId,
            GlobalCaseRole.CREATOR.getRole()
        };

        return template.queryForObject(COUNT_CASE_USERS, parameters, Integer.class);
    }

}
