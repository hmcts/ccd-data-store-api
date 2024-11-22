package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.EventTokenException;

class EventTokenServiceTest {

    @InjectMocks
    private EventTokenService eventTokenService;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseEventDefinition event;

    @Mock
    private JurisdictionDefinition jurisdictionDefinition;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    private String token;
    private String uid;
    private EventTokenProperties eventTokenProperties;


    private AutoCloseable openMocks;

    @BeforeEach
    public void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        token = "token";
        uid = "userId";

        when(applicationParams.getTokenSecret()).thenReturn("secretKey");

        eventTokenProperties = new EventTokenProperties(
            uid,
            "caseId",
            "jurisdictionId",
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            "1"
        );
    }

    @Test
    public void testValidateToken_NullToken() {
        assertThrows(BadRequestException.class, () -> eventTokenService.validateToken(null,uid, caseDetails,
            event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_EmptyToken() {
        assertThrows(BadRequestException.class, () -> eventTokenService.validateToken("", uid, caseDetails,
            event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_ValidTokenAllConditionsMet() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenAllConditionsMetWithNullValues() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties propertiesWithNull = new EventTokenProperties(
            null,
            null,
            null,
            null,
            null,
            "version",
            "caseState",
            "1"
        );

        doReturn(propertiesWithNull).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenConditionMetWithNullEventId() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties eventTokenProperties = new EventTokenProperties(
            uid,
            "caseId",
            "jurisdictionId",
            null,
            "caseTypeId",
            "version",
            "caseState",
            "1"
        );

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenConditionMetWithNullCaseId() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties eventTokenProperties = new EventTokenProperties(
            uid,
            null,
            "jurisdictionId",
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            "1"
        );

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenConditionMetWithNullJurisdictionId() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties eventTokenProperties = new EventTokenProperties(
            uid,
            "caseId",
            null,
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            "1"
        );

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenConditionMetWithNullCaseTypeId() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties eventTokenProperties = new EventTokenProperties(
            uid,
            "caseId",
            "jurisdictionId",
            "eventId",
            null,
            "version",
            "caseState",
            "1"
        );

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_ValidTokenConditionMetWithNullUid() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties eventTokenProperties = new EventTokenProperties(
            null,
            "caseId",
            "jurisdictionId",
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            "1"
        );

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }

    @Test
    public void testValidateToken_InvalidTokenConditionsEventIdNotMet() {
        when(applicationParams.isValidateTokenClaims()).thenReturn(true);
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("differentEventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        assertThrows(EventTokenException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_InvalidTokenConditionsCaseIdNotMet() {
        when(applicationParams.isValidateTokenClaims()).thenReturn(true);
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("differentCaseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        assertThrows(EventTokenException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_InvalidTokenConditionsJurisdictionIdNotMet() {
        when(applicationParams.isValidateTokenClaims()).thenReturn(true);
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("differentJurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        assertThrows(EventTokenException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_InvalidTokenConditionsCaseTypeIdNotMet() {
        when(applicationParams.isValidateTokenClaims()).thenReturn(true);
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("differentCaseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        assertThrows(EventTokenException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_InvalidTokenConditionsUidNotMet() {
        when(applicationParams.isValidateTokenClaims()).thenReturn(true);
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        uid = "differentUid";
        assertThrows(EventTokenException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    public void testValidateToken_DoNothingWhenValidateClaimIsFalseForInvalidTokenConditionsUidNotMet() {
        EventTokenService spyEventTokenService = spy(new EventTokenService(null,
            applicationParams, null));

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        uid = "differentUid";

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(spyEventTokenService, times(1)).parseToken(token);
        verify(caseDetails, times(1)).setVersion(1);
    }

    @Test
    public void testValidateToken_NonNullEntityVersion() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties propertiesWithVersion = new EventTokenProperties(
            uid,
            "caseId",
            "jurisdictionId",
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            "2"
        );

        doReturn(propertiesWithVersion).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(2);
    }

    @Test
    public void testValidateToken_NullEntityVersion() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        EventTokenProperties propertiesWithVersion = new EventTokenProperties(
            uid,
            "caseId",
            "jurisdictionId",
            "eventId",
            "caseTypeId",
            "version",
            "caseState",
            null
        );

        doReturn(propertiesWithVersion).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails, never()).setVersion(null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        openMocks.close();
    }
}
