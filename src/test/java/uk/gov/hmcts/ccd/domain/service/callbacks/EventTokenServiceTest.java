package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.newCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;

class EventTokenServiceTest {

    private static final String TOKEN_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String UID = "123";
    private static final String OTHER_UID = "456";
    private static final String JURISDICTION_ID = "PROBATE";
    private static final String OTHER_JURISDICTION_ID = "DIVORCE";
    private static final String CASE_TYPE_ID = "TestAddressBookCase";
    private static final String OTHER_CASE_TYPE_ID = "OtherCaseType";
    private static final String EVENT_ID = "UPDATE";
    private static final String OTHER_EVENT_ID = "DELETE";
    private static final String CASE_ID = "1";
    private static final String OTHER_CASE_ID = "2";

    @Mock
    private RandomKeyGenerator randomKeyGenerator;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseService caseService;

    private EventTokenService eventTokenService;

    private CaseDetails caseDetails;
    private CaseEventDefinition caseEventDefinition;
    private JurisdictionDefinition jurisdictionDefinition;
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(applicationParams.getTokenSecret()).thenReturn(TOKEN_SECRET);
        when(randomKeyGenerator.generate()).thenReturn("token-id");
        when(caseService.hashData(org.mockito.ArgumentMatchers.any(CaseDetails.class))).thenReturn("case-version");

        eventTokenService = new EventTokenService(randomKeyGenerator, applicationParams, caseService);

        caseDetails = newCaseDetails()
            .withId(CASE_ID)
            .withReference(Long.valueOf(CASE_ID))
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .build();

        caseEventDefinition = newCaseEvent().withId(EVENT_ID).build();
        jurisdictionDefinition = newJurisdiction().withJurisdictionId(JURISDICTION_ID).build();
        caseTypeDefinition = newCaseType().withId(CASE_TYPE_ID).build();
    }

    @Test
    @DisplayName("should validate token when all event token claims match the start trigger")
    void shouldValidateTokenWhenAllEventTokenClaimsMatch() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        assertDoesNotThrow(() -> eventTokenService.validateToken(
            token, UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    @DisplayName("should validate token for new case when case id claim is absent")
    void shouldValidateTokenForNewCaseWhenCaseIdClaimIsAbsent() {
        String token = eventTokenService.generateToken(
            UID, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        assertDoesNotThrow(() -> eventTokenService.validateToken(
            token, UID, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    @DisplayName("should validate token when case id claim uses case reference")
    void shouldValidateTokenWhenCaseIdClaimUsesCaseReference() {
        CaseDetails referencedCase = newCaseDetails()
            .withId("441")
            .withReference(1785925516698138L)
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .build();

        String token = eventTokenService.generateToken(
            UID, referencedCase, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        assertDoesNotThrow(() -> eventTokenService.validateToken(
            token, UID, referencedCase, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    @DisplayName("should validate legacy token when case id claim uses internal entity id")
    void shouldValidateLegacyTokenWhenCaseIdClaimUsesInternalEntityId() {
        CaseDetails referencedCase = newCaseDetails()
            .withId("441")
            .withReference(1785925516698138L)
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .build();

        String legacyToken = Jwts.builder()
            .setId("legacy-token")
            .setSubject(UID)
            .setIssuedAt(new Date())
            .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode(TOKEN_SECRET))
            .claim(EventTokenProperties.CASE_ID, "441")
            .claim(EventTokenProperties.EVENT_ID, EVENT_ID)
            .claim(EventTokenProperties.CASE_TYPE_ID, CASE_TYPE_ID)
            .claim(EventTokenProperties.JURISDICTION_ID, JURISDICTION_ID)
            .compact();

        assertDoesNotThrow(() -> eventTokenService.validateToken(
            legacyToken, UID, referencedCase, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));
    }

    @Test
    @DisplayName("should throw when event id claim does not match even if uid matches")
    void shouldThrowWhenEventIdClaimDoesNotMatchEvenIfUidMatches() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        CaseEventDefinition otherEvent = newCaseEvent().withId(OTHER_EVENT_ID).build();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventTokenService.validateToken(
                token, UID, caseDetails, otherEvent, jurisdictionDefinition, caseTypeDefinition));

        assertEquals("Cannot find matching start trigger", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when case id claim does not match even if uid matches")
    void shouldThrowWhenCaseIdClaimDoesNotMatchEvenIfUidMatches() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        CaseDetails otherCaseDetails = newCaseDetails()
            .withId(OTHER_CASE_ID)
            .withReference(Long.valueOf(OTHER_CASE_ID))
            .withCaseTypeId(CASE_TYPE_ID)
            .withJurisdiction(JURISDICTION_ID)
            .build();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventTokenService.validateToken(
                token, UID, otherCaseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));

        assertEquals("Cannot find matching start trigger", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when jurisdiction id claim does not match even if uid matches")
    void shouldThrowWhenJurisdictionIdClaimDoesNotMatchEvenIfUidMatches() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        JurisdictionDefinition otherJurisdiction = newJurisdiction().withJurisdictionId(OTHER_JURISDICTION_ID).build();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventTokenService.validateToken(
                token, UID, caseDetails, caseEventDefinition, otherJurisdiction, caseTypeDefinition));

        assertEquals("Cannot find matching start trigger", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when case type id claim does not match even if uid matches")
    void shouldThrowWhenCaseTypeIdClaimDoesNotMatchEvenIfUidMatches() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        CaseTypeDefinition otherCaseType = newCaseType().withId(OTHER_CASE_TYPE_ID).build();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventTokenService.validateToken(
                token, UID, caseDetails, caseEventDefinition, jurisdictionDefinition, otherCaseType));

        assertEquals("Cannot find matching start trigger", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when uid claim does not match")
    void shouldThrowWhenUidClaimDoesNotMatch() {
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> eventTokenService.validateToken(
                token, OTHER_UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));

        assertEquals("Cannot find matching start trigger", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when token is missing")
    void shouldThrowWhenTokenIsMissing() {
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> eventTokenService.validateToken(
                null, UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition));

        assertEquals("Missing start trigger token", exception.getMessage());
    }

    @Test
    @DisplayName("should match event token claims case insensitively")
    void shouldMatchEventTokenClaimsCaseInsensitively() {
        CaseEventDefinition lowerCaseEvent = newCaseEvent().withId(EVENT_ID.toLowerCase()).build();
        String token = eventTokenService.generateToken(
            UID, caseDetails, caseEventDefinition, jurisdictionDefinition, caseTypeDefinition);

        assertDoesNotThrow(() -> eventTokenService.validateToken(
            token, UID, caseDetails, lowerCaseEvent, jurisdictionDefinition, caseTypeDefinition));
    }
}
