package uk.gov.hmcts.ccd.data.draft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.EventBuilder;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftBuilder.aCreateCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.GetDraftBuilder.aGetDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftBuilder.anUpdateCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.std.CaseDataContentBuilder.aCaseDataContent;

class DefaultDraftGatewayTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    public static final TextNode VALUE_CLASS = JSON_NODE_FACTORY.textNode("valueClass");
    public static final TextNode VALUE = JSON_NODE_FACTORY.textNode("value");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CASE_DATA_CONTENT = "CaseDataContent";
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    private static final String DID = "5";
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

    private DraftGateway draftGateway;

    Map<String, JsonNode> data = Maps.newHashMap();
    Map<String, JsonNode> dataClassification = Maps.newHashMap();
    Event event = new EventBuilder().build();

    private CaseDataContent caseDataContent;

    private GetDraft getDraft;
    private Draft draft = new DraftBuilder().build();
    private CaseDraft caseDraft;
    private CreateCaseDraft createCaseDraft;
    private UpdateCaseDraft updateCaseDraft;
    private String draftBaseURL = "draftBaseURL";
    private String draftURL5 = "draftBaseURL/" + DID;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        when(applicationParams.draftBaseURL()).thenReturn(draftBaseURL);
        when(applicationParams.draftURL(DID)).thenReturn(draftURL5);
        when(applicationParams.getDraftMaxStaleDays()).thenReturn(7);

        data.put(KEY, VALUE);
        dataClassification.put(KEY_CLASS, VALUE_CLASS);

        caseDataContent = aCaseDataContent()
            .withData(data)
            .withEvent(event)
            .withIgnoreWarning(true)
            .withSecurityClassification(dataClassification)
            .withToken(TOKEN)
            .build();

        getDraft = aGetDraft()
            .withId(DID)
            .withDocument(mapper.writeValueAsString(caseDataContent))
            .withType(TYPE)
            .withCreated(NOW)
            .withUpdated(NOW_PLUS_5_MIN)
            .build();

        caseDraft = new CaseDraftBuilder()
            .withUserId(UID)
            .withJurisdictionId(JID)
            .withCaseTypeId(CTID)
            .withEventTriggerId(ETID)
            .withCaseDataContent(caseDataContent)
            .build();
        createCaseDraft = aCreateCaseDraft()
            .withDocument(caseDraft)
            .withType(CASE_DATA_CONTENT)
            .withMaxStaleDays(applicationParams.getDraftMaxStaleDays())
            .build();
        updateCaseDraft = anUpdateCaseDraft()
            .withDocument(caseDraft)
            .withType(CASE_DATA_CONTENT)
            .build();
        draftGateway = new DefaultDraftGateway(restTemplate, securityUtils, applicationParams);
    }

    @Test
    void shouldSuccessfullySaveToDraft() throws URISyntaxException {
        ResponseEntity<HttpEntity> response = ResponseEntity.created(new URI("http://localhost:8800/drafts/4")).build();
        doReturn(response).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(HttpEntity.class));

        Long result = draftGateway.save(createCaseDraft);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftBaseURL), eq(HttpMethod.POST), any(RequestEntity.class), eq(HttpEntity.class)),
            () -> assertThat(result, is(4L))
        );
    }

    @Test
    void shouldFailToSaveToDraft() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftGateway.save(createCaseDraft));
        assertThat(actualException.getMessage(), is("Problem saving draft because of connectivity issue"));
    }

    @Test
    void shouldSuccessfullyUpdateToDraft() throws URISyntaxException {
        doReturn(ResponseEntity.status(204).build()).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(HttpEntity.class));

        Draft result = draftGateway.update(updateCaseDraft, DID);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.PUT), any(RequestEntity.class), eq(HttpEntity.class)),
            () -> assertThat(result, hasProperty("id", is(Long.valueOf(DID))))
        );
    }

    @Test
    void shouldFailToUpdateToDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftGateway.update(updateCaseDraft, DID));
        assertThat(actualException.getMessage(), is("Problem updating draft because of connectivity issue"));
    }

    @Test
    void shouldFailToUpdateToDraftWhenNotFound() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(HttpEntity.class));

        final ResourceNotFoundException actualException = assertThrows(ResourceNotFoundException.class, () -> draftGateway.update(updateCaseDraft, DID));
        assertThat(actualException.getMessage(), is("Resource not found when getting draft for draftId=5 because of 404 NOT_FOUND"));
    }

    @Test
    void shouldSuccessfullyRetrieveDraft() throws URISyntaxException {
        doReturn(ResponseEntity.ok(getDraft)).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(GetDraft.class));

        Draft result = draftGateway.get(DID);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.GET), any(RequestEntity.class), eq(GetDraft.class)),
            () -> assertThat(result, hasProperty("id", is(Long.valueOf(DID)))),
            () -> assertThat(result, hasProperty("type", is(TYPE))),
            () -> assertThat(result, hasProperty("document", is(equalTo(caseDataContent)))),
            () -> assertThat(result, hasProperty("created", is(NOW.toLocalDateTime()))),
            () -> assertThat(result, hasProperty("updated", is(NOW_PLUS_5_MIN.toLocalDateTime())))
        );
    }


    @Test
    void shouldFailToGetFromDraftWhenConnectivityIssue() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(GetDraft.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftGateway.get(DID));
        assertThat(actualException.getMessage(), is("Problem getting draft because of connectivity issue"));
    }

    @Test
    void shouldFailToGetFromDraftWhenNotFound() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(GetDraft.class));

        final ResourceNotFoundException actualException = assertThrows(ResourceNotFoundException.class, () -> draftGateway.get(DID));
        assertThat(actualException.getMessage(), is("Resource not found when getting draft for draftId=5 because of 404 NOT_FOUND"));
    }

}
