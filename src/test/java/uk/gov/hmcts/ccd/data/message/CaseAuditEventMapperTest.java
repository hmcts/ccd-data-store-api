package uk.gov.hmcts.ccd.data.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class CaseAuditEventMapperTest {
    private static final Long CASE_DATA_ID = 101111L;
    private static final String CASE_TYPE_ID = "121212";
    private static final String EVENT_NAME = "eventName";
    private static final String EVENT_ID = "eventID";
    private static final String JURISDICTION_ID = "jurisdiction";
    private static final String USER_ID = "USER_ID";
    private static final String NEW_STATE = "state2";
    private static final String PREVIOUS_STATE = "state1";
    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(1987, 12, 4, 17, 30);;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should map model to entity ")
    public void modelTo() {
        MessageCandidateMapper messageCandidateMapper = new MessageCandidateMapper();
        MessageQueueCandidate messageQueueCandidate = getMessageQueueCandidate();
        MessageQueueCandidateEntity result = messageCandidateMapper.modelToEntity(messageQueueCandidate);

        MessageInformation messageInformation = getMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        assertEquals(node, result.getMessageInformation());
        assertEquals(EVENT_NAME, result.getMessageType());
        assertEquals(TIMESTAMP, result.getTimeStamp());
        assertEquals(null, result.getPublished());
    }

    private MessageQueueCandidate getMessageQueueCandidate() {
        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();

        MessageInformation messageInformation = getMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        messageQueueCandidate.setMessageType(EVENT_NAME);
        messageQueueCandidate.setMessageInformation(node);
        messageQueueCandidate.setTimeStamp(TIMESTAMP);

        return messageQueueCandidate;
    }

    private MessageInformation getMessageInformation() {
        final MessageInformation messageInformation= new MessageInformation();

        messageInformation.setCaseId(CASE_DATA_ID.toString());
        messageInformation.setJurisdictionId(JURISDICTION_ID);
        messageInformation.setCaseTypeId(CASE_TYPE_ID);
        messageInformation.setEventInstanceId(EVENT_ID);
        messageInformation.setEventTimestamp(TIMESTAMP);
        messageInformation.setEventId(EVENT_ID);
        messageInformation.setUserId(USER_ID);
        messageInformation.setPreviousStateId(PREVIOUS_STATE);
        messageInformation.setNewStateId(NEW_STATE);

        return messageInformation;
    }
}
