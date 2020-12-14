package uk.gov.hmcts.ccd.domain.service.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.message.MessageCandidateRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.MessageQueueCandidate;

import javax.inject.Inject;

@Service
@Qualifier("caseEventMessageService")
public class CaseEventMessageService extends AbstractMessageService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final MessageCandidateRepository messageCandidateRepository;

    @Inject
    public CaseEventMessageService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                   final MessageCandidateRepository messageCandidateRepository,
                                   CaseAuditEventRepository caseAuditEventRepository) {
        super(userRepository, caseAuditEventRepository);
        this.messageCandidateRepository = messageCandidateRepository;
    }

    @Override
    public void handleMessage(MessageContext messageContext) {
        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();
        if (Boolean.TRUE.equals(messageContext.getCaseEventDefinition().getPublish())) {

            MessageInformation messageInformation = populateMessageInformation(messageContext);
            JsonNode node = mapper.convertValue(messageInformation, JsonNode.class);

            messageQueueCandidate.setMessageInformation(node);
            messageQueueCandidate.setMessageType(MessageType.CASE_EVENT.name());
            messageCandidateRepository.save(messageQueueCandidate);
        }
    }
}
