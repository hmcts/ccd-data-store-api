package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.EventPostStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class DefaultCreateEventOperationTest {

    private static final String JURISDICTION_ID = "SSCS";
    private static final String CASE_TYPE_ID = "Claim";
    private static final String CASE_REFERENCE = "1234123412341236";
    private static final String TOKEN = "eygeyvcey12w2";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String EVENT_ID = "UpdateCase";
    private static final String PRE_STATE_ID = "Created";
    private static final String POST_STATE = "Updated";
    private static final Integer VERSION_NUMBER = 1;
    private static final LocalDateTime LAST_MODIFIED = LocalDateTime.of(2015, 12, 21, 15, 30);
    private static final String CALLBACK_URL = "http://sscs.reform.hmcts.net/callback";

    @Mock
    private EventValidator eventValidator;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private CreateCaseEventService createEventService;

    @InjectMocks
    private DefaultCreateEventOperation createEventOperation;

    private Event event;

    private Map<String, JsonNode> data;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;
    private CaseDetails caseDetailsBefore;
    private CaseStateDefinition postState;
    private CaseDataContent caseDataContent;

    private static Event buildEvent() {
        final Event event = anEvent().build();
        event.setEventId(EVENT_ID);
        event.setSummary("Update case summary");
        event.setDescription("Update case description");
        return event;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        event = buildEvent();
        data = buildJsonNodeData();
        caseDataContent = newCaseDataContent().withEvent(event).withData(data).withToken(TOKEN)
            .withIgnoreWarning(IGNORE_WARNING).build();
        final JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(JURISDICTION_ID);
        final Version version = new Version();
        version.setNumber(VERSION_NUMBER);
        caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(CASE_TYPE_ID);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        caseTypeDefinition.setVersion(version);

        caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setPostStates(getEventPostStates(POST_STATE));

        caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setState(PRE_STATE_ID);
        caseDetails.setLastModified(LAST_MODIFIED);
        caseDetailsBefore = mock(CaseDetails.class);
        postState = new CaseStateDefinition();
        postState.setId(POST_STATE);

        CreateCaseEventResult caseEventResult =  CreateCaseEventResult.caseEventWith()
            .caseDetailsBefore(caseDetailsBefore)
            .savedCaseDetails(caseDetails)
            .eventTrigger(caseEventDefinition)
            .build();

        given(createEventService.createCaseEvent(CASE_REFERENCE, caseDataContent)).willReturn(caseEventResult);

    }

    private List<EventPostStateDefinition> getEventPostStates(String... postStateReferences) {
        List<EventPostStateDefinition> postStates = new ArrayList<>();
        int i = 0;
        for (String reference : postStateReferences) {
            EventPostStateDefinition definition = new EventPostStateDefinition();
            definition.setPostStateReference(reference);
            definition.setPriority(++i);
            postStates.add(definition);
        }
        return postStates;
    }

    @Test
    @DisplayName("should invoke after submit callback")
    void shouldInvokeAfterSubmitCallback() {
        caseEventDefinition.setCallBackURLSubmittedEvent(CALLBACK_URL);
        AfterSubmitCallbackResponse response = new AfterSubmitCallbackResponse();
        response.setConfirmationHeader("Header");
        response.setConfirmationBody("Body");
        doReturn(ResponseEntity.ok(response)).when(callbackInvoker)
            .invokeSubmittedCallback(caseEventDefinition,
                caseDetailsBefore,
                caseDetails);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertAll(
            () -> verify(callbackInvoker).invokeSubmittedCallback(caseEventDefinition, caseDetailsBefore,
                this.caseDetails),
            () -> assertThat(caseDetails.getAfterSubmitCallbackResponse().getConfirmationHeader(), is("Header")),
            () -> assertThat(caseDetails.getAfterSubmitCallbackResponse().getConfirmationBody(), is("Body")),
            () -> assertThat(caseDetails.getCallbackResponseStatusCode(), is(SC_OK)),
            () -> assertThat(caseDetails.getCallbackResponseStatus(), is("CALLBACK_COMPLETED"))
        );
    }

    @Test
    @DisplayName("should return incomplete response status if remote endpoint is down")
    void shouldReturnIncomplete() {
        caseEventDefinition.setCallBackURLSubmittedEvent(CALLBACK_URL);
        doThrow(new CallbackException("Testing failure")).when(callbackInvoker)
            .invokeSubmittedCallback(caseEventDefinition,
                caseDetailsBefore,
                caseDetails);

        final CaseDetails caseDetails = createEventOperation.createCaseEvent(CASE_REFERENCE, caseDataContent);

        assertAll(
            () -> assertNull(caseDetails.getAfterSubmitCallbackResponse()),
            () -> assertThat(caseDetails.getCallbackResponseStatusCode(), is(SC_OK)),
            () -> assertThat(caseDetails.getCallbackResponseStatus(), is("INCOMPLETE_CALLBACK"))
        );
    }

    private Map<String, JsonNode> buildJsonNodeData() {
        return new HashMap<>();
    }

}
