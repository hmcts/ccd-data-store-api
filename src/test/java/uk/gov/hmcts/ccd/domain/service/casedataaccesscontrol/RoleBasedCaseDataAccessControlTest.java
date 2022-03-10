package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.data.caseaccess.GlobalCaseRole.CREATOR;

class RoleBasedCaseDataAccessControlTest {

    private static final String IDAM_ID = "23";
    private static final String CASE_ID = "45677";
    private final List<String> caseRoles = asList("[CASE_ROLE_1]", "[CASE_ROLE_2]");
    private static final Long CASE_REFERENCE = 1234123412341234L;


    @Mock
    private CaseUserRepository caseUserRepository;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleBasedCaseDataAccessControl instance;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("when creator has access level GRANTED, then it should grant access to creator")
    void shouldGrantAccessToAccessLevelGrantedCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.GRANTED);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        instance.grantAccess(caseDetails, IDAM_ID);

        verify(caseUserRepository).grantAccess(Long.valueOf(CASE_ID), IDAM_ID, CREATOR.getRole());
    }

    @Test
    @DisplayName("when creator has access level ALL, then it should NOT grant access to creator")
    void shouldNotGrantAccessToAccessLevelAllCreator() {
        when(userAuthorisation.getAccessLevel()).thenReturn(UserAuthorisation.AccessLevel.ALL);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        instance.grantAccess(caseDetails, IDAM_ID);

        verifyZeroInteractions(caseUserRepository);
    }

    @Test
    @DisplayName("should return valid accessProfiles for case reference")
    void shouldReturnValidAccessProfilesForCaseReference() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setId(CASE_ID);
        when(caseDetailsRepository.findByReference(CASE_REFERENCE.toString()))
            .thenReturn(Optional.of(caseDetails));
        when(userRepository.getUserId()).thenReturn(IDAM_ID);
        when(caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID), IDAM_ID)).thenReturn(caseRoles);
        Set<AccessProfile> accessProfileList =
            instance.generateAccessProfilesByCaseReference(CASE_REFERENCE.toString());
        assertEquals(2, accessProfileList.size());
    }

    @Test
    @DisplayName("should return valid accessProfiles for caseId")
    void shouldReturnValidAccessProfilesForCaseId() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setId(CASE_ID);
        when(caseDetailsRepository.findByReference(CASE_REFERENCE.toString())).thenReturn(Optional.empty());
        when(caseDetailsRepository.findById(null, Long.parseLong(CASE_ID)))
            .thenReturn(Optional.of(caseDetails));
        when(userRepository.getUserId()).thenReturn(IDAM_ID);
        when(caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID), IDAM_ID)).thenReturn(caseRoles);
        Set<AccessProfile> accessProfileList = instance.generateAccessProfilesByCaseReference(CASE_ID);
        assertEquals(2, accessProfileList.size());
    }

    @Test
    @DisplayName("should throw exception when case reference or case id is invalid")
    void shouldThrowException() {
        when(caseDetailsRepository.findByReference(CASE_REFERENCE.toString())).thenReturn(Optional.empty());
        when(caseDetailsRepository.findById(null, Long.parseLong(CASE_ID)))
            .thenReturn(Optional.empty());
        when(userRepository.getUserId()).thenReturn(IDAM_ID);
        when(caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID), IDAM_ID)).thenReturn(caseRoles);
        assertThrows(CaseNotFoundException.class, () -> instance.generateAccessProfilesByCaseReference(CASE_ID));
    }
}
