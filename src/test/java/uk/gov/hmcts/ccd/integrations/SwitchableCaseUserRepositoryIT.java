package uk.gov.hmcts.ccd.integrations;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.caseaccess.*;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Ignore
@Transactional
public class SwitchableCaseUserRepositoryIT extends IntegrationTest {

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate template;

    @Inject
    protected DataSource db;

    @Mock
    protected CaseDetailsRepository caseDetailsRepository;

    @MockBean
    private CaseUserAuditRepository auditRepository;

    @Inject
    protected CCDCaseUserRepository ccdCaseUserRepository;

    @Inject
    protected AMCaseUserRepository amCaseUserRepository;

    @InjectMocks
    private AMSwitch amSwitch;

    @Mock
    private ApplicationParams goodApplicationParams;

    @Inject
    private SwitchableCaseUserRepository switchableCaseUserRepository;

    private static final String JURISDICTION = "CMC";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final String CCD_CASE_TYPE_ID = "DIVORCE";
    private static final String AM_CASE_TYPE_ID = "PROBATE";
    private static final String CCD_CASE_TYPE_ID_2 = "CMC";
    private static final String AM_CASE_TYPE_ID_2 = "CR";
    private static final String BOTH_CASE_TYPE_ID = "FR";
    private static final String BOTH_CASE_TYPE_ID_2 = "TEST";
    private static final Long CCD_CASE_REFERENCE = 1234123456781236L;
    private static final Long CCD_CASE_REFERENCE_2 = 1234123456781239L;
    private static final Long AM_CASE_REFERENCE = 1234123456781237L;
    private static final Long AM_CASE_REFERENCE_2 = 1234123456781231L;
    private static final Long BOTH_CASE_REFERENCE = 1234123456781238L;
    private static final Long BOTH_CASE_REFERENCE_2 = 1234123456781212L;
    private static final String USER_ID = "132";
    private static final String USER_ID_2 = "1324";
    private static final Long CCD_CASE_ID = 465L;
    private static final Long CCD_CASE_ID_2 = 364L;
    private static final Long AM_CASE_ID = 798L;
    private static final Long AM_CASE_ID_2 = 718L;
    private static final Long BOTH_CASE_ID = 132L;
    private static final Long BOTH_CASE_ID_2 = 152L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String CASE_ROLE_GRANTED = "[ALREADY_GRANTED]";
    private static final String CASE_ROLE_SOLICITOR = "[SOLICITOR]";
    private static final String DIVORCE_CT = "DIVORCE";
    private static final String PROBATE_CT = "PROBATE";
    private static final String CMC_CT = "CMC";
    private static final String FR_CT = "FR";
    private static final String TEST_CT = "TEST";
    private static final String CR_CT = "CR";

    private List<String> ccdOnlyWriteCaseTypes = Lists.newArrayList(DIVORCE_CT, CMC_CT);
    private List<String> amOnlyWriteCaseTypes = Lists.newArrayList(PROBATE_CT, CR_CT);
    private List<String> bothWriteCaseTypes = Lists.newArrayList(FR_CT, TEST_CT);
    private List<String> ccdOnlyReadCaseTypes = Lists.newArrayList(DIVORCE_CT, CR_CT, TEST_CT);
    private List<String> amOnlyReadCaseTypes = Lists.newArrayList(PROBATE_CT, CMC_CT, FR_CT);

    private List<Long> caseIds;

    private List<String> caseRoles;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        template = new JdbcTemplate(db);

        // predefine the CaseDetailsRepository(mocked Object) to return the respective case details for the given Jurisdiction and Case Reference combination
        predefineCaseDetailsRepository();

        // configure the AM Switch values to predefine the Case Type ID's to set Read & write access from CCD & AM
        preDefineAMSwitchValues();
    }

    @Test
    @DisplayName("To validate the preconfigured Switch values for the Case Types Defined in this Test Class")
    public void validatePreConfiguredSwitchValuesForCaseTypesTest() {

        assertTrue(amSwitch.isReadAccessManagementWithAM(AM_CASE_TYPE_ID));
        assertTrue(amSwitch.isReadAccessManagementWithCCD(CCD_CASE_TYPE_ID));
        assertTrue(amSwitch.isWriteAccessManagementWithAM(AM_CASE_TYPE_ID));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID));
        assertTrue(amSwitch.isReadAccessManagementWithCCD(AM_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isReadAccessManagementWithAM(CCD_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isWriteAccessManagementWithAM(AM_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(CCD_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isReadAccessManagementWithAM(BOTH_CASE_TYPE_ID));
        assertTrue(amSwitch.isReadAccessManagementWithCCD(BOTH_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID));

        assertFalse(amSwitch.isReadAccessManagementWithCCD(AM_CASE_TYPE_ID));
        assertFalse(amSwitch.isReadAccessManagementWithAM(CCD_CASE_TYPE_ID));
        assertFalse(amSwitch.isWriteAccessManagementWithAM(CCD_CASE_TYPE_ID_2));
        assertFalse(amSwitch.isWriteAccessManagementWithCCD(AM_CASE_TYPE_ID_2));
        assertFalse(amSwitch.isReadAccessManagementWithCCD(BOTH_CASE_TYPE_ID));
        assertFalse(amSwitch.isReadAccessManagementWithAM(BOTH_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isWriteAccessManagementWithAM(BOTH_CASE_TYPE_ID_2));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(BOTH_CASE_TYPE_ID_2));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("To Test writing the data into CCD and validate the Read from AM & CCD")
    public void ccdOnlyWriteAndValidateReadFromAMAndCCD() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID,CCD_CASE_REFERENCE,USER_ID).size(),equalTo(0));

        // Grant access for the given Case, User & role.
        switchableCaseUserRepository.grantAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID , USER_ID, CASE_ROLE_GRANTED);

        // Read the case data & validate that the grant access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CCD_CASE_ID));
        caseRoles = switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID,CCD_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        // change the AM Switch values to swap the Read access b/w CCD & AM. Also assert the values to validate that the changes are effective.
        changeAMSwitchValuesToSwapRead();

        // Post the AM Switch change :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        // Revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        //revert the AM Switch value back to original config
        preDefineAMSwitchValues();

        //read the case data to validate that the revoke access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
    }

    //@Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("To Test writing the data into AM and validate the Read from AM & CCD")
    public void amOnlyWriteAndValidateReadFromAMAndCCD() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID,AM_CASE_ID,USER_ID).size(),equalTo(0));

        // Grant case user access
        switchableCaseUserRepository.grantAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID , USER_ID, CASE_ROLE_GRANTED);

        // Read the case data & validate that the grant access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(AM_CASE_ID));
        caseRoles = switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID,AM_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        // change the AM Switch values to swap the Read access b/w CCD & AM. Also assert the values to validate that the changes are effective.
        changeAMSwitchValuesToSwapRead();

        // Post the AM Switch change :- Retrieve & validate the current size / volume of cases for the User
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        // Revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        //revert the AM Switch value back to original config
        preDefineAMSwitchValues();

        //read the case data to validate that the revoke access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
    }

    //@Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("To Test writing the data into both CCD & AM and validate the Read from AM & CCD")
    public void WriteToBothAndValidateReadFromAMAndCCD() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        // Grant access for the given Case, User & role.
        switchableCaseUserRepository.grantAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        // Read the case data & validate that the grant access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(BOTH_CASE_ID));

        // change the AM Switch values to swap the Read access b/w CCD & AM. Also assert the values to validate that the changes are effective.
        changeAMSwitchValuesToSwapRead();

        // Post the AM Switch change :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(BOTH_CASE_ID));

        // Revoke access for the given Case, User & role.
        switchableCaseUserRepository.revokeAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        //read the case data to validate that the revoke access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

    }

    //@Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("To read multiple case data for single user and validate the Read from AM & CCD")
    public void readMultipleCaseDataForSingleUser() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        // Grant case user access
        switchableCaseUserRepository.grantAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
        switchableCaseUserRepository.grantAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_SOLICITOR);

        // Read the case data & validate that the grant access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(2));
        assertThat(caseIds, containsInAnyOrder(CCD_CASE_ID,AM_CASE_ID));

        caseRoles = switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID,CCD_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_SOLICITOR));

        caseRoles = switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID,AM_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        // revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        //read the case data to validate that the revoke access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CCD_CASE_ID));

        // revoke the case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_SOLICITOR);

        //read the case data to validate that the revoke access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
    }


    //@Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("To read multiple case data for multiple users and validate the Read from AM & CCD")
    public void readMultipleCaseDataForMultipleUsers() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        //grant case user access
        switchableCaseUserRepository.grantAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
        switchableCaseUserRepository.grantAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_SOLICITOR);
        switchableCaseUserRepository.grantAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
        switchableCaseUserRepository.grantAccess(JURISDICTION, BOTH_CASE_REFERENCE_2.toString(), BOTH_CASE_ID_2, USER_ID_2, CASE_ROLE_SOLICITOR);

        // Read the case data & validate that the grant access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(3));
        assertThat(caseIds, containsInAnyOrder(CCD_CASE_ID,AM_CASE_ID,BOTH_CASE_ID));

        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID_2);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(BOTH_CASE_ID_2));

        caseRoles = switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID,CCD_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_SOLICITOR));

        caseRoles = switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID,AM_CASE_ID,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        caseRoles = switchableCaseUserRepository.findCaseRoles(BOTH_CASE_TYPE_ID_2,BOTH_CASE_ID_2,USER_ID_2);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        //revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, AM_CASE_REFERENCE.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);

        //read the case data to validate that the revoke access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(2));
        assertThat(caseIds, containsInAnyOrder(CCD_CASE_ID,BOTH_CASE_ID));

        //revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_SOLICITOR);

        //read the case data to validate that the revoke access change is effective
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(BOTH_CASE_ID));

        //revoke case user access
        switchableCaseUserRepository.revokeAccess(JURISDICTION, BOTH_CASE_REFERENCE.toString(), BOTH_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
        switchableCaseUserRepository.revokeAccess(JURISDICTION, BOTH_CASE_REFERENCE_2.toString(), BOTH_CASE_ID_2, USER_ID_2, CASE_ROLE_SOLICITOR);

        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID_2).size(), equalTo(0));


    }

    //@Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("Validate invalid switch condition - Write with 'TO_AM_ONLY' & read 'FROM_CCD'")
    public void validateInvalidSwitchConfigForAM() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        //grant case user access
        switchableCaseUserRepository.grantAccess(JURISDICTION, AM_CASE_REFERENCE_2.toString(), AM_CASE_ID_2, USER_ID, CASE_ROLE_GRANTED);

        // Read the case data & validate that the grant access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID_2,AM_CASE_ID_2,USER_ID).size(),equalTo(0));

        // change the AM Switch values to swap the Read access b/w CCD & AM. Also assert the values to validate that the changes are effective.
        changeAMSwitchValuesToSwapRead();

        // Post the AM Switch change :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(AM_CASE_ID_2));
        caseRoles = switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID_2,AM_CASE_ID_2,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CASE_ROLE_GRANTED));

        // Revoke access for the given Case, User & role.
        switchableCaseUserRepository.revokeAccess(JURISDICTION, AM_CASE_REFERENCE_2.toString(), AM_CASE_ID_2, USER_ID, CASE_ROLE_GRANTED);

        // Post Revoke access :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(AM_CASE_TYPE_ID_2,AM_CASE_ID_2,USER_ID).size(),equalTo(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("Validate invalid switch condition - Write with 'TO_CCD_ONLY' & read 'FROM_AM'")
    public void validateInvalidSwitchConfigForCCD() {

        // validate the initial size / volume of cases/roles for the User, Case Type & Case Reference combination
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));

        //grant case user access
        switchableCaseUserRepository.grantAccess(JURISDICTION, CCD_CASE_REFERENCE_2.toString(), CCD_CASE_ID_2, USER_ID, CASE_ROLE_GRANTED);

        // Read the case data & validate that the grant access change is effective
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID_2,CCD_CASE_ID_2,USER_ID).size(),equalTo(0));

        // change the AM Switch values to swap the Read access b/w CCD & AM. Also assert the values to validate that the changes are effective.
        changeAMSwitchValuesToSwapRead();

        // Post the AM Switch change :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        caseIds = switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID);
        assertThat(caseIds.size(), equalTo(1));
        assertThat(caseIds.get(0), equalTo(CCD_CASE_ID_2));
        caseRoles = switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID_2,CCD_CASE_ID_2,USER_ID);
        assertThat(caseRoles.size(), equalTo(1));
        assertThat(caseRoles.get(0), equalTo(CASE_ROLE_GRANTED));

        // Revoke access for the given Case, User & role.
        switchableCaseUserRepository.revokeAccess(JURISDICTION, CCD_CASE_REFERENCE_2.toString(), CCD_CASE_ID_2, USER_ID, CASE_ROLE_GRANTED);

        // Post Revoke access :- Retrieve & validate the current size / volume of cases/roles for the User, Case Type & Case Reference combination.
        assertThat(switchableCaseUserRepository.findCasesUserIdHasAccessTo(USER_ID).size(), equalTo(0));
        assertThat(switchableCaseUserRepository.findCaseRoles(CCD_CASE_TYPE_ID_2,CCD_CASE_ID_2,USER_ID).size(),equalTo(0));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("should throw CaseNotFound Exception when granting/revoking access for invalid Case reference")
    public void shouldThrowErrorForInvalidCaseReference() {

        assertAll(
            () -> assertThrows(CaseNotFoundException.class, () -> {
                switchableCaseUserRepository.grantAccess(JURISDICTION, CASE_NOT_FOUND.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            }),
            () -> assertThrows(CaseNotFoundException.class, () -> {
                switchableCaseUserRepository.revokeAccess(JURISDICTION, CASE_NOT_FOUND.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            })
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases_am_switch_test.sql"})
    @DisplayName("should throw CaseNotFound Exception when Case reference is in different jurisdiction")
    public void shouldThrowErrorInvalidJurisdiction() {

        assertAll(
            () -> assertThrows(CaseNotFoundException.class, () -> {
                switchableCaseUserRepository.grantAccess(WRONG_JURISDICTION, CASE_NOT_FOUND.toString(), CCD_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            }),
            () -> assertThrows(CaseNotFoundException.class, () -> {
                switchableCaseUserRepository.revokeAccess(WRONG_JURISDICTION, CASE_NOT_FOUND.toString(), AM_CASE_ID, USER_ID, CASE_ROLE_GRANTED);
            })
        );
    }

    private Optional<CaseDetails> configureCaseDetailRepository(Long caseReference, Long caseId, String caseTypeId) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(caseId));
        caseDetails.setReference(caseReference);
        caseDetails.setCaseTypeId(caseTypeId);
        return Optional.of(caseDetails);
    }

    private CaseDetails configureCaseDetailRepositoryData(Long caseReference, Long caseId, String caseTypeId) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(caseId));
        caseDetails.setReference(caseReference);
        caseDetails.setCaseTypeId(caseTypeId);
        return caseDetails;
    }

    private void predefineCaseDetailsRepository() {
        doReturn(configureCaseDetailRepository(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID)).when(caseDetailsRepository).findByReference(JURISDICTION,CCD_CASE_REFERENCE);
        doReturn(configureCaseDetailRepository(CCD_CASE_REFERENCE_2, CCD_CASE_ID_2, CCD_CASE_TYPE_ID_2)).when(caseDetailsRepository).findByReference(JURISDICTION,CCD_CASE_REFERENCE_2);
        doReturn(configureCaseDetailRepository(AM_CASE_REFERENCE, AM_CASE_ID, AM_CASE_TYPE_ID)).when(caseDetailsRepository).findByReference(JURISDICTION,AM_CASE_REFERENCE);
        doReturn(configureCaseDetailRepository(AM_CASE_REFERENCE_2, AM_CASE_ID_2, AM_CASE_TYPE_ID_2)).when(caseDetailsRepository).findByReference(JURISDICTION,AM_CASE_REFERENCE_2);
        doReturn(configureCaseDetailRepository(BOTH_CASE_REFERENCE, BOTH_CASE_ID, BOTH_CASE_TYPE_ID)).when(caseDetailsRepository).findByReference(JURISDICTION,BOTH_CASE_REFERENCE);
        doReturn(configureCaseDetailRepository(BOTH_CASE_REFERENCE_2, BOTH_CASE_ID_2, BOTH_CASE_TYPE_ID_2)).when(caseDetailsRepository).findByReference(JURISDICTION,BOTH_CASE_REFERENCE_2);
        doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(JURISDICTION,CASE_NOT_FOUND);
        doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(WRONG_JURISDICTION,CCD_CASE_REFERENCE);
        doReturn(configureCaseDetailRepositoryData(CCD_CASE_REFERENCE, CCD_CASE_ID, CCD_CASE_TYPE_ID)).when(caseDetailsRepository).findById(CCD_CASE_ID);
        doReturn(configureCaseDetailRepositoryData(CCD_CASE_REFERENCE_2, CCD_CASE_ID_2, CCD_CASE_TYPE_ID_2)).when(caseDetailsRepository).findById(CCD_CASE_ID_2);
        doReturn(configureCaseDetailRepositoryData(AM_CASE_REFERENCE, AM_CASE_ID, AM_CASE_TYPE_ID)).when(caseDetailsRepository).findById(AM_CASE_ID);
        doReturn(configureCaseDetailRepositoryData(AM_CASE_REFERENCE_2, AM_CASE_ID_2, AM_CASE_TYPE_ID_2)).when(caseDetailsRepository).findById(AM_CASE_ID_2);
        doReturn(configureCaseDetailRepositoryData(BOTH_CASE_REFERENCE, BOTH_CASE_ID, BOTH_CASE_TYPE_ID)).when(caseDetailsRepository).findById(BOTH_CASE_ID);
        doReturn(configureCaseDetailRepositoryData(BOTH_CASE_REFERENCE_2, BOTH_CASE_ID_2, BOTH_CASE_TYPE_ID_2)).when(caseDetailsRepository).findById(BOTH_CASE_ID_2);
    }

    private void changeAMSwitchValuesToSwapRead() {
        doReturn(ccdOnlyWriteCaseTypes).when(goodApplicationParams).getWriteToCCDCaseTypesOnly();
        doReturn(amOnlyWriteCaseTypes).when(goodApplicationParams).getWriteToAMCaseTypesOnly();
        doReturn(bothWriteCaseTypes).when(goodApplicationParams).getWriteToBothCaseTypes();
        doReturn(amOnlyReadCaseTypes).when(goodApplicationParams).getReadFromCCDCaseTypes();
        doReturn(ccdOnlyReadCaseTypes).when(goodApplicationParams).getReadFromAMCaseTypes();
        amSwitch = new AMSwitch(goodApplicationParams);
        switchableCaseUserRepository = new SwitchableCaseUserRepository(caseDetailsRepository, ccdCaseUserRepository, amCaseUserRepository, amSwitch);
    }

    private void preDefineAMSwitchValues() {
        doReturn(ccdOnlyWriteCaseTypes).when(goodApplicationParams).getWriteToCCDCaseTypesOnly();
        doReturn(amOnlyWriteCaseTypes).when(goodApplicationParams).getWriteToAMCaseTypesOnly();
        doReturn(bothWriteCaseTypes).when(goodApplicationParams).getWriteToBothCaseTypes();
        doReturn(ccdOnlyReadCaseTypes).when(goodApplicationParams).getReadFromCCDCaseTypes();
        doReturn(amOnlyReadCaseTypes).when(goodApplicationParams).getReadFromAMCaseTypes();
        amSwitch = new AMSwitch(goodApplicationParams);
        switchableCaseUserRepository = new SwitchableCaseUserRepository(caseDetailsRepository, ccdCaseUserRepository, amCaseUserRepository, amSwitch);
    }

}
