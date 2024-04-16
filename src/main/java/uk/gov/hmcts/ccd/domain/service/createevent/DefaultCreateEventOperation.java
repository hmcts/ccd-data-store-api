package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Qualifier("default")
public class DefaultCreateEventOperation implements CreateEventOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCreateEventOperation.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final EventValidator eventValidator;
    private final CreateCaseEventService createEventService;
    private final CallbackInvoker callbackInvoker;

    @Inject
    public DefaultCreateEventOperation(final EventValidator eventValidator,
                                       final CreateCaseEventService createEventService,
                                       final CallbackInvoker callbackInvoker) {
        this.createEventService = createEventService;
        this.eventValidator = eventValidator;
        this.callbackInvoker = callbackInvoker;
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /*
     * ==== Log message. ====
     */
    private String jcLog(final String message) {
        String rc;
        try {
            final String url = "https://ccd-data-store-api-pr-2356.preview.platform.hmcts.net/jcdebug";
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");
            // Write the string payload to the HTTP request body
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            rc = "Response Code: " + connection.getResponseCode();
        } catch (Exception e) {
            rc = "EXCEPTION";
            e.printStackTrace();
        }
        return "jcLog: " + rc;
    }

    @Transactional
    @Override
    public CaseDetails createCaseEvent(final String caseReference,
                                       final CaseDataContent content) {
        jcLog("JCDEBUG2: createCaseEvent:77");

        eventValidator.validate(content.getEvent());

        final CreateCaseEventResult caseEventResult = createEventService.createCaseEvent(caseReference, content);

        if (!isBlank(caseEventResult.getEventTrigger().getCallBackURLSubmittedEvent())) {
            return invokeSubmitedToCallback(caseEventResult);
        }
        return caseEventResult.getSavedCaseDetails();
    }

    @Transactional
    @Override
    public CaseDetails createCaseSystemEvent(final String caseReference,
                                             final Integer version,
                                             final String attributePath,
                                             final String categoryId) {
        Event event = createDocumentUpdatedEvent();
        eventValidator.validate(event);

        final CreateCaseEventResult caseEventResult = createEventService
            .createCaseSystemEvent(caseReference, attributePath, categoryId, event);

        if (!isBlank(caseEventResult.getEventTrigger().getCallBackURLSubmittedEvent())) {
            return invokeSubmitedToCallback(caseEventResult);
        }
        return caseEventResult.getSavedCaseDetails();
    }

    private Event createDocumentUpdatedEvent() {
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        return event;
    }

    private CaseDetails invokeSubmitedToCallback(CreateCaseEventResult caseEventResult) {
        CaseDetails caseDetails = caseEventResult.getSavedCaseDetails();
        try { // make a call back
            final ResponseEntity<AfterSubmitCallbackResponse> callBackResponse = callbackInvoker
                .invokeSubmittedCallback(caseEventResult.getEventTrigger(), caseEventResult.getCaseDetailsBefore(),
                    caseEventResult.getSavedCaseDetails());
            caseDetails.setAfterSubmitCallbackResponseEntity(callBackResponse);
        } catch (CallbackException ex) {
            LOG.warn("Submitted callback failed", ex);
            // Exception occurred, e.g. call back service is unavailable,
            caseDetails.setIncompleteCallbackResponse();
        }
        return caseDetails;
    }

}
