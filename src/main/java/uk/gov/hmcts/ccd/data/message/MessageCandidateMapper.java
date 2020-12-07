package uk.gov.hmcts.ccd.data.message;

import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class MessageCandidateMapper {

    public MessageQueueCandidate entityToModel(final MessageQueueCandidateEntity messageQueueCandidateEntity) {
        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();

        messageQueueCandidate.setMessageType(messageQueueCandidateEntity.getMessageType());
        messageQueueCandidate.setTimeStamp(messageQueueCandidateEntity.getTimeStamp());
        messageQueueCandidate.setMessageInformation(messageQueueCandidateEntity.getMessageInformation());
        messageQueueCandidate.setPublished(messageQueueCandidateEntity.getPublished());

        return messageQueueCandidate;
    }

    public MessageQueueCandidateEntity modelToEntity(final MessageQueueCandidate messageQueueCandidate) {
        final MessageQueueCandidateEntity messageQueueCandidateEntity = new MessageQueueCandidateEntity();

        messageQueueCandidateEntity.setMessageInformation(messageQueueCandidate.getMessageInformation());
        messageQueueCandidateEntity.setTimeStamp(messageQueueCandidate.getTimeStamp());
        messageQueueCandidateEntity.setMessageType(messageQueueCandidate.getMessageType());
        messageQueueCandidateEntity.setPublished(messageQueueCandidate.getPublished());

        return messageQueueCandidateEntity;
    }
}
