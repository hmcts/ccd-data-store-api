package uk.gov.hmcts.ccd.data.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

class MessageCandidateMapperTest {
    private static final Long CASE_DATA_ID = 101111L;
    private static final String MESSAGE_TYPE = "CASE_EVENT";
    private static final String CASE_TYPE_ID = "121212";
    private static final String EVENT_ID = "eventName";
    private static final Long EVENT_INSTNACE_ID = 2L;
    private static final String STATE_NAME = "stateName";
    private static final String JURISDICTION = "Jurisdiction";
    private static final String USER_ID = "USER_ID";
    private static final LocalDateTime DATE_TIME =
        LocalDateTime.of(2020,12,07,16,42,00);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should map model to entity ")
    void modelTo() {
        LocalDateTime timestamp = LocalDateTime.now();
        MessageCandidateMapper messageCandidateMapper = new MessageCandidateMapper();
        MessageQueueCandidate messageCandidate = getMessageCandidate(timestamp);
        MessageQueueCandidateEntity result = messageCandidateMapper.modelToEntity(messageCandidate);
        MessageInformation messageInformation = populateMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        assertNull(result.getPublished());
        assertEquals(MESSAGE_TYPE, result.getMessageType());
        assertEquals(node, result.getMessageInformation());
        assertEquals(timestamp, result.getTimeStamp());
    }

    @Test
    @DisplayName("Should map entity to model ")
    void entityTo() {
        LocalDateTime timestamp = LocalDateTime.now();
        MessageCandidateMapper messageCandidateMapper = new MessageCandidateMapper();
        MessageQueueCandidateEntity messageQueueCandidateEntity = getMessageCandidateEntity(timestamp);
        MessageInformation messageInformation = populateMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        MessageQueueCandidate result = messageCandidateMapper.entityToModel(messageQueueCandidateEntity);

        assertNull(result.getPublished());
        assertEquals(MESSAGE_TYPE, result.getMessageType());
        assertEquals(node, result.getMessageInformation());
        assertEquals(timestamp, result.getTimeStamp());
    }

    private MessageQueueCandidateEntity getMessageCandidateEntity(LocalDateTime timestamp) {
        MessageInformation messageInformation = populateMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        MessageQueueCandidateEntity messageQueueCandidateEntity = new MessageQueueCandidateEntity();
        messageQueueCandidateEntity.setMessageInformation(node);
        messageQueueCandidateEntity.setMessageType(MESSAGE_TYPE);
        messageQueueCandidateEntity.setTimeStamp(timestamp);
        return messageQueueCandidateEntity;
    }

    private MessageQueueCandidate getMessageCandidate(LocalDateTime timestamp) {
        MessageInformation messageInformation = populateMessageInformation();
        JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();
        messageQueueCandidate.setMessageInformation(node);
        messageQueueCandidate.setMessageType("CASE_EVENT");
        messageQueueCandidate.setTimeStamp(timestamp);
        return messageQueueCandidate;
    }

    private MessageInformation populateMessageInformation() {

        final MessageInformation messageInformation = new MessageInformation();

        messageInformation.setCaseId(CASE_DATA_ID.toString());
        messageInformation.setJurisdictionId(JURISDICTION);
        messageInformation.setCaseTypeId(CASE_TYPE_ID);
        messageInformation.setEventInstanceId(EVENT_INSTNACE_ID);
        messageInformation.setEventTimestamp(DATE_TIME);
        messageInformation.setEventId(EVENT_ID);
        messageInformation.setUserId(USER_ID);
        messageInformation.setPreviousStateId(STATE_NAME);
        messageInformation.setNewStateId(STATE_NAME);

        return messageInformation;
    }
}
