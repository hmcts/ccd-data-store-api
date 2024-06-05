package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.RandomKeyGenerator;

class EventTokenServiceTest {

    @InjectMocks
    private EventTokenService eventTokenService;

    @Mock
    private RandomKeyGenerator randomKeyGenerator;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseService caseService;

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
        token = "validToken";
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
        assertThrows(BadRequestException.class, () -> {
            eventTokenService.validateToken(null, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);
        });
    }

    @Test
    public void testValidateToken_EmptyToken() {
        assertThrows(BadRequestException.class, () -> {
            eventTokenService.validateToken("", uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);
        });
    }

    @Test
    public void testValidateToken_ValidTokenAllConditionsMet() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("eventId");
        when(caseDetails.getId()).thenReturn("caseId");
        when(jurisdictionDefinition.getId()).thenReturn("jurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("caseTypeId");

        // Mock the parseToken method
        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(Integer.parseInt(eventTokenProperties.getEntityVersion()));
    }



    @Test
    public void testValidateToken_InvalidTokenConditionsNotMet() {
        EventTokenService spyEventTokenService = spy(eventTokenService);

        when(event.getId()).thenReturn("differentEventId");
        when(caseDetails.getId()).thenReturn("differentCaseId");
        when(jurisdictionDefinition.getId()).thenReturn("differentJurisdictionId");
        when(caseTypeDefinition.getId()).thenReturn("differentCaseTypeId");

        // Mock the parseToken method
        doReturn(eventTokenProperties).when(spyEventTokenService).parseToken(token);

        assertThrows(ResourceNotFoundException.class,  () -> spyEventTokenService.validateToken(token, uid,
            caseDetails, event, jurisdictionDefinition, caseTypeDefinition));
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

        // Mock the parseToken method
        doReturn(propertiesWithVersion).when(spyEventTokenService).parseToken(token);

        spyEventTokenService.validateToken(token, uid, caseDetails, event, jurisdictionDefinition, caseTypeDefinition);

        verify(caseDetails).setVersion(2);
    }

    @AfterEach
    public void tearDown() throws Exception {
        openMocks.close();
    }
}
