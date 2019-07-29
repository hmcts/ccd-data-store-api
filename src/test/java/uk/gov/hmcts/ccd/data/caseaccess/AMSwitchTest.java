package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

class AMSwitchTest {

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private AMSwitch amSwitch;

    public static final String UNSPECIFIED = "Unspecified";
    private static final String DIVORCE_CT = "DIVORCE";
    private static final String PROBATE_CT = "PROBATE";
    private static final String CMC_CT = "CMC";
    private static final String FR_CT = "FR";
    private static final String TEST_CT = "TEST";
    private List<String> ccdOnlyWriteCaseTypes = Lists.newArrayList(DIVORCE_CT, CMC_CT, TEST_CT);
    private List<String> amOnlyWriteCaseTypes = Lists.newArrayList(PROBATE_CT);
    private List<String> bothWriteCaseTypes = Lists.newArrayList(FR_CT);
    private List<String> ccdOnlyReadCaseTypes = Lists.newArrayList(CMC_CT, PROBATE_CT);
    private List<String> amOnlyReadCaseTypes = Lists.newArrayList(DIVORCE_CT, FR_CT, TEST_CT);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(ccdOnlyWriteCaseTypes).when(applicationParams).getWriteToCCDCaseTypesOnly();
        doReturn(amOnlyWriteCaseTypes).when(applicationParams).getWriteToAMCaseTypesOnly();
        doReturn(bothWriteCaseTypes).when(applicationParams).getWriteToBothCaseTypes();
        doReturn(ccdOnlyReadCaseTypes).when(applicationParams).getReadFromCCDCaseTypes();
        doReturn(amOnlyReadCaseTypes).when(applicationParams).getReadFromAMCaseTypes();

        amSwitch = new AMSwitch(applicationParams);
    }

    @Test
    void shouldGrantCCDOnlyWriteAccessIfCaseTypeNotSpecified() {
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(UNSPECIFIED));
        assertFalse(amSwitch.isWriteAccessManagementWithAM(UNSPECIFIED));
    }

    @Test
    void shouldGrantCCDOnlyWriteAccess() {
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(DIVORCE_CT));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(CMC_CT));
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(TEST_CT));

        assertFalse(amSwitch.isWriteAccessManagementWithCCD(PROBATE_CT));
    }

    @Test
    void shouldGrantAMOnlyWriteAccess() {
        assertTrue(amSwitch.isWriteAccessManagementWithAM(PROBATE_CT));

        assertFalse(amSwitch.isWriteAccessManagementWithAM(CMC_CT));
        assertFalse(amSwitch.isWriteAccessManagementWithAM(DIVORCE_CT));
        assertFalse(amSwitch.isWriteAccessManagementWithAM(TEST_CT));
    }

    @Test
    void shouldGrantBothWriteAccessEvenIfCCDOnlyOrAMOnlyDoesNotHaveItSpecified() {
        assertTrue(amSwitch.isWriteAccessManagementWithCCD(FR_CT));
        assertTrue(amSwitch.isWriteAccessManagementWithAM(FR_CT));
    }

    @Test
    void shouldGrantCCDOnlyReadAccessIfCaseTypeNotSpecified() {
        assertTrue(amSwitch.isReadAccessManagementWithCCD(UNSPECIFIED));
        assertFalse(amSwitch.isReadAccessManagementWithAM(UNSPECIFIED));
    }

    @Test
    void shouldGrantCCDOnlyReadAccess() {
        assertTrue(amSwitch.isReadAccessManagementWithCCD(CMC_CT));
        assertTrue(amSwitch.isReadAccessManagementWithCCD(PROBATE_CT));

        assertFalse(amSwitch.isReadAccessManagementWithCCD(DIVORCE_CT));
        assertFalse(amSwitch.isReadAccessManagementWithCCD(FR_CT));
        assertFalse(amSwitch.isReadAccessManagementWithCCD(TEST_CT));
    }

    @Test
    void shouldGrantAMOnlyReadAccess() {
        assertTrue(amSwitch.isReadAccessManagementWithAM(DIVORCE_CT));
        assertTrue(amSwitch.isReadAccessManagementWithAM(FR_CT));
        assertTrue(amSwitch.isReadAccessManagementWithAM(TEST_CT));

        assertFalse(amSwitch.isReadAccessManagementWithAM(CMC_CT));
        assertFalse(amSwitch.isReadAccessManagementWithAM(PROBATE_CT));
    }

}
