package uk.gov.hmcts.ccd.data.caseaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.data.helper.AccessManagementQueryHelper;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;
import uk.gov.hmcts.reform.amlib.models.ResourceDefinition;

import javax.sql.DataSource;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static uk.gov.hmcts.reform.amlib.enums.AccessType.ROLE_BASED;
import static uk.gov.hmcts.reform.amlib.enums.RoleType.IDAM;
import static uk.gov.hmcts.reform.amlib.enums.SecurityClassification.PUBLIC;

@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AccessManagementQueryHelper.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AMCaseUserRepositoryComponentTest extends BaseTest {

    private static final String JURISDICTION_ID = "JURISDICTION";
    private static final String CASE_TYPE_ID = "CASE_TYPE";
    private static final String CASE_REFERENCE = "1234123412341234";
    private static final Long CASE_ID = 1L;
    private static final Long CASE_ID_GRANTED = 2L;
    private static final Long CASE_ID_3 = 3L;
    private static final String USER_ID = "89000";
    private static final String USER_ID_GRANTED = "89001";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_SOLICITOR = "[SOLICITOR]";
    private static final String CASE_ROLE_CREATOR = "[CREATOR]";

    @Autowired
    private AMCaseUserRepository repository;

    @Autowired
    private AccessManagementQueryHelper accessManagementQueryHelper;

    @Autowired
    DefaultRoleSetupImportService defaultRoleSetupImportService;

    @Autowired
    @Qualifier("amDataSource")
    private DataSource dataSource;

    @Before
    public void setUp() {

        defaultRoleSetupImportService = new DefaultRoleSetupImportService(dataSource);

        defaultRoleSetupImportService.addService(JURISDICTION_ID);
        defaultRoleSetupImportService.addRole(CASE_ROLE, IDAM, PUBLIC, ROLE_BASED);
        defaultRoleSetupImportService.addRole(CASE_ROLE_SOLICITOR, IDAM, PUBLIC, ROLE_BASED);
        defaultRoleSetupImportService.addRole(CASE_ROLE_CREATOR, IDAM, PUBLIC, ROLE_BASED);

        ResourceDefinition resourceDefinition =
            //TODO: What should be the resourceType and resourceName.
            //To be clarified with Mutlu/Shashank again.//resource name: CMC, FPL
            new ResourceDefinition(JURISDICTION_ID, "case", CASE_REFERENCE);
        defaultRoleSetupImportService.addResourceDefinition(resourceDefinition);
    }

    @After
    public void tearDown() {
        accessManagementQueryHelper.deleteAllFromAccessManagementTables();
    }

    @Test
    public void shouldGrantAccessAsCustomCaseRole() {
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);
        Integer records = accessManagementQueryHelper.findExplicitAccessPermissions(JURISDICTION_ID);

        assertThat(records, equalTo(1));
    }

    @Test
    public void shouldRevokeAccessAsCustomCaseRole() {
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);
        repository.revokeAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);

        Integer records = accessManagementQueryHelper.findExplicitAccessPermissions(JURISDICTION_ID);
        assertThat(records, equalTo(0));
    }

    @Test
    public void shouldFindCasesUserIdHasAccessTo() {
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);
        List<Long> caseIds = repository.findCasesUserIdHasAccessTo(USER_ID);

        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CASE_ID));

        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID_GRANTED, CASE_ROLE);
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID_3, USER_ID_GRANTED, CASE_ROLE);
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID_GRANTED, USER_ID_GRANTED, CASE_ROLE);

        caseIds = repository.findCasesUserIdHasAccessTo(USER_ID_GRANTED);

        assertThat(caseIds.size(), equalTo(3));
        assertThat(caseIds, containsInAnyOrder(CASE_ID, CASE_ID_GRANTED, CASE_ID_3));
    }

    @Test
    public void shouldFindCaseRolesUserPerformsForCase() {
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE_CREATOR);
        List<String> caseRoles = repository.findCaseRoles(CASE_TYPE_ID, CASE_ID, USER_ID);

        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_CREATOR));

        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE);
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE_CREATOR);
        repository.grantAccess(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE, CASE_ID, USER_ID, CASE_ROLE_SOLICITOR);

        caseRoles = repository.findCaseRoles(CASE_TYPE_ID, CASE_ID, USER_ID);

        assertThat(caseRoles.size(), equalTo(3));
        assertThat(caseRoles, containsInAnyOrder(CASE_ROLE, CASE_ROLE_CREATOR, CASE_ROLE_SOLICITOR));
    }
}
