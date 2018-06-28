package uk.gov.hmcts.ccd.data.draft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
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

class DefaultDraftRepositoryTest {

    private static final String CASE_DATA_CONTENT = "CaseDataContent";
    private static final String UID = "1";
    private static final String JID = "TEST";
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "createCase";

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
    private String draftBaseURL = "draftBaseURL";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn(new HttpHeaders()).when(securityUtils).userAuthorizationHeaders();
        when(applicationParams.draftBaseURL()).thenReturn(draftBaseURL);
        when(applicationParams.getDraftMaxStaleDays()).thenReturn(7);

        caseDataContentDraft = new CaseDataContentDraft(UID, JID, CTID, ETID, caseDataContent);
        createCaseDataContentDraft =  new CreateCaseDataContentDraft(caseDataContentDraft, CASE_DATA_CONTENT, applicationParams.getDraftMaxStaleDays());
        draftRepository = new DefaultDraftRepository(restTemplate, securityUtils, applicationParams);
    }


    @Test
    void shouldSuccessfullySaveToDraft() throws URISyntaxException {
        ResponseEntity<HttpEntity> response = ResponseEntity.created(new URI("http://localhost:8800/drafts/4")).build();
        doReturn(response).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(HttpEntity.class));

        Draft result = draftRepository.set(createCaseDataContentDraft);

        assertAll(
            () ->  verify(restTemplate).exchange(eq(draftBaseURL), eq(HttpMethod.POST), any(RequestEntity.class), eq(HttpEntity.class)),
            () ->  assertThat(result, hasProperty("id", is(4L)))
        );
    }

    @Test
    void shouldFailToSaveToDraft() {
        Exception exception = new RestClientException("connectivity issue");
        doThrow(exception).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(HttpEntity.class));

        final ServiceException actualException = assertThrows(ServiceException.class, () -> draftRepository.set(createCaseDataContentDraft));
        assertThat(actualException.getMessage(), is("Problem saving draft=" + createCaseDataContentDraft + " because of connectivity issue"));
    }
}
