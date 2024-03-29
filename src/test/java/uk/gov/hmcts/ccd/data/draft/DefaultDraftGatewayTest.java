package uk.gov.hmcts.ccd.data.draft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDraftBuilder.newCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CreateCaseDraftBuilder.newCreateCaseDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DraftBuilder.anDraft;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.UpdateCaseDraftBuilder.newUpdateCaseDraft;

class DefaultDraftGatewayTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    public static final TextNode VALUE_CLASS = JSON_NODE_FACTORY.textNode("valueClass");
    public static final TextNode VALUE = JSON_NODE_FACTORY.textNode("value");
    private static final String CASE_DATA_CONTENT = "CaseDataContent";
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    private static final String DID = "4433221";
    public static final String TYPE = "caseDataContent";
    public static final ZonedDateTime NOW = ZonedDateTime.now();
    public static final ZonedDateTime NOW_PLUS_5_MIN = ZonedDateTime.now().plus(5, ChronoUnit.MINUTES);
    public static final String TOKEN = "testToken";
    public static final String KEY = "key";
    public static final String KEY_CLASS = "keyClass";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplate createDraftRestTemplate;

    @Mock
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    private DraftGateway draftGateway;

    private Map<String, JsonNode> data = Maps.newHashMap();
    private Map<String, JsonNode> dataClassification = Maps.newHashMap();
    private final String securityClassification = SecurityClassification.PRIVATE.name();
    private final Event event = anEvent().build();

    private CaseDataContent caseDataContent;

    private Draft draft;
    private CaseDraft caseDraft;
    private CreateCaseDraftRequest createCaseDraftRequest;
    private UpdateCaseDraftRequest updateCaseDraftRequest;
    private final String draftBaseURL = "draftBaseURL";
    private final String draftURL5 = "draftBaseURL/" + DID;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        when(applicationParams.draftBaseURL()).thenReturn(draftBaseURL);
        when(applicationParams.draftURL(DID)).thenReturn(draftURL5);
        when(applicationParams.getDraftMaxTTLDays()).thenReturn(7);

        data.put(KEY, VALUE);
        dataClassification.put(KEY_CLASS, VALUE_CLASS);

        caseDataContent = newCaseDataContent()
            .withData(data)
            .withEvent(event)
            .withIgnoreWarning(true)
            .withDataClassification(dataClassification)
            .withSecurityClassification(securityClassification)
            .withToken(TOKEN)
            .build();
        caseDraft = newCaseDraft()
            .withUserId(UID)
            .withJurisdictionId(JID)
            .withCaseTypeId(CTID)
            .withEventId(ETID)
            .withCaseDataContent(caseDataContent)
            .build();
        draft = anDraft()
            .withId(DID)
            .withType(TYPE)
            .withDocument(JacksonUtils.convertValueJsonNode(caseDraft))
            .withCreated(NOW)
            .withUpdated(NOW_PLUS_5_MIN)
            .build();
        createCaseDraftRequest = newCreateCaseDraft()
            .withDocument(caseDraft)
            .withType(CASE_DATA_CONTENT)
            .withTTLDays(applicationParams.getDraftMaxTTLDays())
            .build();
        updateCaseDraftRequest = newUpdateCaseDraft()
            .withDocument(caseDraft)
            .withType(CASE_DATA_CONTENT)
            .build();
        draftGateway = new DefaultDraftGateway(createDraftRestTemplate, restTemplate, securityUtils, applicationParams,
            draftResponseToCaseDetailsBuilder);
    }

    @Test
    void shouldSuccessfullyCreateDraft() throws URISyntaxException {
        ResponseEntity<HttpEntity> response = ResponseEntity.created(new URI("http://localhost:8800/drafts/4")).build();
        doReturn(response).when(createDraftRestTemplate).exchange(anyString(), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(HttpEntity.class));

        Long result = draftGateway.create(createCaseDraftRequest);

        assertAll(
            () -> verify(createDraftRestTemplate).exchange(eq(draftBaseURL), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(HttpEntity.class)),
            () -> verify(restTemplate, never()).exchange(eq(draftBaseURL), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(HttpEntity.class)),
            () -> assertEquals(4L, result)
        );
    }

    @Test
    void shouldFailToCreateDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(createDraftRestTemplate).exchange(anyString(), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException =
            assertThrows(ServiceException.class, () -> draftGateway.create(createCaseDraftRequest));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToGetResponseFromCreateDraft() {
        ResponseEntity<HttpEntity> response = ResponseEntity.noContent().build();
        doReturn(response).when(createDraftRestTemplate).exchange(anyString(), eq(HttpMethod.POST),
            any(HttpEntity.class), eq(HttpEntity.class));

        assertNull(draftGateway.create(createCaseDraftRequest));
    }

    @Test
    void shouldSuccessfullyUpdateToDraft() {
        doReturn(ResponseEntity.status(204).build()).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT),
            any(HttpEntity.class), eq(HttpEntity.class));

        DraftResponse result = draftGateway.update(updateCaseDraftRequest, DID);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.PUT), any(HttpEntity.class),
                eq(HttpEntity.class)),
            () -> verify(createDraftRestTemplate, never()).exchange(eq(draftURL5), eq(HttpMethod.PUT),
                any(RequestEntity.class), eq(HttpEntity.class)),
            () -> assertEquals(DID, result.getId())
        );
    }

    @Test
    void shouldFailToUpdateToDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(HttpEntity.class));

        final ServiceException actualException =
            assertThrows(ServiceException.class, () -> draftGateway.update(updateCaseDraftRequest, DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToUpdateToDraftWhenOtherClientError() {
        Exception exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(HttpEntity.class));

        final ApiException actualException =
            assertThrows(ApiException.class, () -> draftGateway.update(updateCaseDraftRequest, DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToUpdateToDraftWhenNotFound() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(HttpEntity.class));

        final ResourceNotFoundException actualException =
            assertThrows(ResourceNotFoundException.class, () -> draftGateway.update(updateCaseDraftRequest, DID));
        assertEquals("No draft found ( draft reference = '4433221' )", actualException.getMessage());
    }

    @Test
    void shouldSuccessfullyRetrieveDraft() {
        doReturn(ResponseEntity.ok(draft)).when(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.GET),
            any(HttpEntity.class), eq(Draft.class));

        DraftResponse result = draftGateway.get(DID);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(Draft.class)),
            () -> verify(createDraftRestTemplate, never()).exchange(eq(draftURL5), eq(HttpMethod.GET),
                any(RequestEntity.class), eq(Draft.class)),
            () -> assertEquals(DID, result.getId()),
            () -> assertEquals(TYPE, result.getType()),
            () -> assertEquals(caseDraft.getUserId(), result.getDocument().getUserId()),
            () -> assertEquals(caseDraft.getJurisdictionId(), result.getDocument().getJurisdictionId()),
            () -> assertEquals(caseDraft.getCaseTypeId(), result.getDocument().getCaseTypeId()),
            () -> assertEquals(caseDraft.getEventId(), result.getDocument().getEventId()),
            () -> assertEquals(caseDataContent.getData(), result.getDocument().getCaseDataContent().getData()),
            () -> assertEquals(caseDataContent.getSecurityClassification(),
                result.getDocument().getCaseDataContent().getSecurityClassification()),
            () -> assertEquals(caseDataContent.getDataClassification(),
                result.getDocument().getCaseDataContent().getDataClassification()),
            () -> assertEquals(caseDataContent.getToken(), result.getDocument().getCaseDataContent().getToken()),
            () -> assertEquals(caseDataContent.getIgnoreWarning(),
                result.getDocument().getCaseDataContent().getIgnoreWarning()),
            () -> assertEquals(caseDataContent.getEvent().getEventId(),
                result.getDocument().getCaseDataContent().getEvent().getEventId()),
            () -> assertEquals(caseDataContent.getEvent().getDescription(),
                result.getDocument().getCaseDataContent().getEvent().getDescription()),
            () -> assertEquals(caseDataContent.getEvent().getSummary(),
                result.getDocument().getCaseDataContent().getEvent().getSummary()),
            () -> assertEquals(NOW.toLocalDateTime(), result.getCreated()),
            () -> assertEquals(NOW_PLUS_5_MIN.toLocalDateTime(), result.getUpdated())
        );
    }

    @Test
    void shouldFailToGetFromDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Draft.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftGateway.get(DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToGetFromDraftWhenOtherClientError() {
        Exception exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Draft.class));

        final ApiException actualException = assertThrows(ApiException.class, () -> draftGateway.get(DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToGetFromDraftWhenNotFound() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(Draft.class));

        final ResourceNotFoundException actualException = assertThrows(ResourceNotFoundException.class, () ->
            draftGateway.get(DID));
        assertEquals("No draft found ( draft reference = '4433221' )", actualException.getMessage());
    }

    @Test
    void shouldSuccessfullyDeleteDraft() {
        doReturn(ResponseEntity.ok(draft)).when(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.DELETE),
            any(HttpEntity.class), eq(Draft.class));

        draftGateway.delete(DID);

        verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Draft.class));
    }

    @Test
    void shouldFailToDeleteFromDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class),
            eq(Draft.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftGateway.delete(DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToDeleteFromDraftWhenOtherClientError() {
        Exception exception = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class),
            eq(Draft.class));

        final ApiException actualException = assertThrows(ApiException.class, () -> draftGateway.delete(DID));
        assertEquals("The draft service is currently down, please refresh your browser or try again later",
            actualException.getMessage());
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldFailToDeleteFromDraftWhenNotFound() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class),
            eq(Draft.class));

        final ResourceNotFoundException actualException = assertThrows(ResourceNotFoundException.class, () ->
            draftGateway.delete(DID));
        assertEquals("No draft found ( draft reference = '4433221' )", actualException.getMessage());
    }

}
