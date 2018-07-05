package uk.gov.hmcts.ccd.data.draft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraftBuilder.aCreateCaseDraftBuilder;
import static uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDataContentDraftBuilder.anUpdateCaseDraftBuilder;

class DefaultDraftRepositoryTest {

    private static final String CASE_DATA_CONTENT = "CaseDataContent";
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";
    private static final String DID = "5";

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    private DraftRepository draftRepository;

    private Draft draft = new Draft();
    private CaseDataContent caseDataContent = new CaseDataContent();
    private CaseDataContentDraft caseDataContentDraft;
    private CreateCaseDataContentDraft createCaseDataContentDraft;
    private UpdateCaseDataContentDraft updateCaseDataContentDraft;
    private String draftBaseURL = "draftBaseURL";
    private String draftURL5 = "draftBaseURL/" + DID;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        when(applicationParams.draftBaseURL()).thenReturn(draftBaseURL);
        when(applicationParams.draftURL(DID)).thenReturn(draftURL5);
        when(applicationParams.getDraftMaxStaleDays()).thenReturn(7);

        caseDataContentDraft = new CaseDraftBuilder().withUserId(UID).withJurisdictionId(JID).withCaseTypeId(CTID).withEventTriggerId(ETID).withCaseDataContent(
            caseDataContent).build();
        createCaseDataContentDraft = aCreateCaseDraftBuilder()
            .withDocument(caseDataContentDraft)
            .withType(CASE_DATA_CONTENT)
            .withMaxStaleDays(applicationParams.getDraftMaxStaleDays())
            .build();
        updateCaseDataContentDraft = anUpdateCaseDraftBuilder()
            .withDocument(caseDataContentDraft)
            .withType(CASE_DATA_CONTENT)
            .build();
        draftRepository = new DefaultDraftRepository(restTemplate, securityUtils, applicationParams);
    }

    @Test
    void shouldSuccessfullySaveToDraft() throws URISyntaxException {
        ResponseEntity<HttpEntity> response = ResponseEntity.created(new URI("http://localhost:8800/drafts/4")).build();
        doReturn(response).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(HttpEntity.class));

        Draft result = draftRepository.save(createCaseDataContentDraft);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftBaseURL), eq(HttpMethod.POST), any(RequestEntity.class), eq(HttpEntity.class)),
            () -> assertThat(result, hasProperty("id", is(4L)))
        );
    }

    @Test
    void shouldFailToSaveToDraft() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftRepository.save(createCaseDataContentDraft));
        assertThat(actualException.getMessage(), is("Problem saving draft because of connectivity issue"));
    }

    @Test
    void shouldSuccessfullyUpdateToDraft() throws URISyntaxException {
        doReturn(ResponseEntity.status(204).build()).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(HttpEntity.class));

        Draft result = draftRepository.update(updateCaseDataContentDraft, DID);

        assertAll(
            () -> verify(restTemplate).exchange(eq(draftURL5), eq(HttpMethod.PUT), any(RequestEntity.class), eq(HttpEntity.class)),
            () -> assertThat(result, hasProperty("id", is(Long.valueOf(DID))))
        );
    }

    @Test
    void shouldFailToUpdateToDraft() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftRepository.update(updateCaseDataContentDraft, DID));
        assertThat(actualException.getMessage(), is("Problem updating draft because of connectivity issue"));
    }
}
