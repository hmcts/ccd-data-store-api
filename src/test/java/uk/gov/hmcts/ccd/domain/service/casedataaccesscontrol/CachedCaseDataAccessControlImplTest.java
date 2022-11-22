package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachedCaseDataAccessControlImplTest {

    private static final String USER_ID = "USER_ID";
    private static final String CASE_ID = "45677";

    private static final String CASE_TYPE_1 = "TEST_CASE_TYPE";

    @Mock
    private NoCacheCaseDataAccessControl noCacheCaseDataAccessControl;

    @InjectMocks
    private CachedCaseDataAccessControlImpl cachedCaseDataAccessControl;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should call generate access profiles by case type id only once")
    void callMethodOnCaseDataAccessControl() {
        cachedCaseDataAccessControl.generateAccessProfilesByCaseTypeId(CASE_TYPE_1);
        cachedCaseDataAccessControl.generateAccessProfilesByCaseTypeId(CASE_TYPE_1);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(1)).generateAccessProfilesByCaseTypeId(CASE_TYPE_1)
        );
    }

    @Test
    @DisplayName("Should call Generate Access Profiles organisations only once")
    void shouldCallGenerateAccessProfilesOrganisationalOnlyOnce() {
        cachedCaseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_1);
        cachedCaseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_1);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(1)).generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_1)
        );
    }

    @Test
    @DisplayName("Should call Generate Access Profiles by reference only once")
    void shouldCallGenerateAccessProfilesByReferenceOnlyOnce() {
        cachedCaseDataAccessControl.generateAccessProfilesByCaseReference(CASE_ID);
        cachedCaseDataAccessControl.generateAccessProfilesByCaseReference(CASE_ID);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(1)).generateAccessProfilesByCaseReference(CASE_ID)
        );
    }

    @Test
    @DisplayName("Should call Generate Access Profiles for restricted case twice when no case reference")
    void shouldCallGenerateAccessProfilesForRestrictedCaseTwiceWhenNoCaseReference() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        cachedCaseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
        cachedCaseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).generateAccessProfilesForRestrictedCase(caseDetails)
        );
    }

    @Test
    @DisplayName("Should call Generate Access Profiles for restricted case only once")
    void shouldCallGenerateAccessProfilesForRestrictedCaseOnlyOnceWithCaseReference() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_ID);
        cachedCaseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
        cachedCaseDataAccessControl.generateAccessProfilesForRestrictedCase(caseDetails);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(1)).generateAccessProfilesForRestrictedCase(caseDetails)
        );
    }

    @Test
    @DisplayName("Should call grant access method of case data access control multiple times")
    void shouldCallGrantAccessMultipleTimesNoCache() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        cachedCaseDataAccessControl.grantAccess(caseDetails, USER_ID);
        cachedCaseDataAccessControl.grantAccess(caseDetails, USER_ID);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).grantAccess(caseDetails, USER_ID)
        );
    }

    @Test
    @DisplayName("Should call generate meta data of case data access control multiple times")
    void shouldCallGenerateMetaDataMultipleTimesNoCache() {
        cachedCaseDataAccessControl.generateAccessMetadata(CASE_ID);
        cachedCaseDataAccessControl.generateAccessMetadata(CASE_ID);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).generateAccessMetadata(CASE_ID)
        );
    }

    @Test
    @DisplayName("Should call generate meta data  with no case id of case data access control multiple times")
    void shouldCallGenerateMetadataWithNoCaseIdMultipleTimesNoCache() {
        cachedCaseDataAccessControl.generateAccessMetadataWithNoCaseId();
        cachedCaseDataAccessControl.generateAccessMetadataWithNoCaseId();
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).generateAccessMetadataWithNoCaseId()
        );
    }

    @Test
    @DisplayName("Should call Any Access Profile Equal to of case data access control multiple times")
    void shouldCallAnyAccessProfileEqualToMultipleTimeNoCache() {
        cachedCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, "access-profile1");
        cachedCaseDataAccessControl.anyAccessProfileEqualsTo(CASE_TYPE_1, "access-profile1");
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).anyAccessProfileEqualsTo(CASE_TYPE_1, "access-profile1")
        );
    }

    @Test
    @DisplayName("Should call remove case definition of case data access control multiple times")
    void shouldRemoveCaseDefinition() {
        Set<AccessProfile> accessProfiles = new HashSet<>();
        Predicate<AccessControlList> access = mock(Predicate.class);
        cachedCaseDataAccessControl.shouldRemoveCaseDefinition(accessProfiles, access, CASE_TYPE_1);
        cachedCaseDataAccessControl.shouldRemoveCaseDefinition(accessProfiles, access, CASE_TYPE_1);
        assertAll(
            () -> verify(noCacheCaseDataAccessControl,
                times(2)).shouldRemoveCaseDefinition(accessProfiles, access, CASE_TYPE_1)
        );
    }
}
