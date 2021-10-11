package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Sets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttributeBasedSecurityClassificationServiceTest {

    private static final String CASE_REFERENCE = "57869987767";
    private static final String CASE_TYPE_ID = "FT_MASTER_CASE_TYP";

    private AttributeBasedSecurityClassificationService classUnderTest;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        classUnderTest = new AttributeBasedSecurityClassificationService(userRepository, caseDataAccessControl);
    }

    @Test
    void shouldReturnEmptyClassificationWhenNoAccessProfilesExistsForUsersCaseReferences() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);

        when(caseDataAccessControl.generateAccessProfilesByCaseReference(CASE_REFERENCE)).thenReturn(null);
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, false);
        assertTrue(classification.isEmpty());
    }

    @Test
    void shouldReturnClassificationWhenAccessProfilesExistsForUsersCaseReferences() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getSecurityClassification()).thenReturn("PUBLIC");

        when(caseDataAccessControl.generateAccessProfilesByCaseReference(CASE_REFERENCE))
            .thenReturn(Sets.newHashSet(accessProfile));
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, false);
        assertEquals(classification.get(), SecurityClassification.PUBLIC);
    }

    @Test
    void shouldReturnMaxClassificationWhenAccessProfilesExistsForUsersCaseReferences() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getSecurityClassification()).thenReturn("PUBLIC");

        AccessProfile privateAccessProfile = mock(AccessProfile.class);
        when(privateAccessProfile.getSecurityClassification()).thenReturn("PRIVATE");

        when(caseDataAccessControl.generateAccessProfilesByCaseReference(CASE_REFERENCE))
            .thenReturn(Sets.newHashSet(accessProfile, privateAccessProfile));
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, false);
        assertEquals(classification.get(), SecurityClassification.PRIVATE);
    }

    @Test
    void shouldReturnEmptyClassificationWhenNoAccessProfilesExistsForUsersCaseTypeId() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);
        when(caseDetails.getCaseTypeId()).thenReturn(CASE_TYPE_ID);

        when(caseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_ID)).thenReturn(null);
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, true);
        assertTrue(classification.isEmpty());
    }


    @Test
    void shouldReturnClassificationWhenAccessProfilesExistsForUsersCaseTypeId() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);
        when(caseDetails.getCaseTypeId()).thenReturn(CASE_TYPE_ID);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getSecurityClassification()).thenReturn("PUBLIC");

        when(caseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_ID))
            .thenReturn(Sets.newHashSet(accessProfile));
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, true);
        assertEquals(classification.get(), SecurityClassification.PUBLIC);
    }

    @Test
    void shouldReturnMaxClassificationWhenAccessProfilesExistsForUsersCaseTypeId() {
        CaseDetails caseDetails = mock(CaseDetails.class);
        when(caseDetails.getReferenceAsString()).thenReturn(CASE_REFERENCE);
        when(caseDetails.getCaseTypeId()).thenReturn(CASE_TYPE_ID);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getSecurityClassification()).thenReturn("PUBLIC");

        AccessProfile privateAccessProfile = mock(AccessProfile.class);
        when(privateAccessProfile.getSecurityClassification()).thenReturn("PRIVATE");

        when(caseDataAccessControl.generateOrganisationalAccessProfilesByCaseTypeId(CASE_TYPE_ID))
            .thenReturn(Sets.newHashSet(accessProfile, privateAccessProfile));
        Optional<SecurityClassification> classification =  classUnderTest.getUserClassification(caseDetails, true);
        assertEquals(classification.get(), SecurityClassification.PRIVATE);
    }
}
