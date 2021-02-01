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
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlockGenerator;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DataBlockGenerator;

import javax.inject.Inject;

@Service
@Qualifier("caseEventMessageService")
public class CaseEventMessageService extends AbstractMessageService {

    private final ObjectMapper objectMapper;

    private final MessageCandidateRepository messageCandidateRepository;

    @Inject
    public CaseEventMessageService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                   final MessageCandidateRepository messageCandidateRepository,
                                   CaseAuditEventRepository caseAuditEventRepository,
                                   DefinitionBlockGenerator definitionBlockGenerator,
                                   DataBlockGenerator dataBlockGenerator,
                                   @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper) {
        super(userRepository, caseAuditEventRepository, definitionBlockGenerator, dataBlockGenerator);
        this.messageCandidateRepository = messageCandidateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleMessage(MessageContext messageContext) {
        final MessageQueueCandidate messageQueueCandidate = new MessageQueueCandidate();
        if (Boolean.TRUE.equals(messageContext.getCaseEventDefinition().getPublish())) {

            MessageInformation messageInformation = populateMessageInformation(messageContext);
            JsonNode node = objectMapper.convertValue(messageInformation, JsonNode.class);

            messageQueueCandidate.setMessageInformation(node);
            messageQueueCandidate.setMessageType(MessageType.CASE_EVENT.name());
            messageCandidateRepository.save(messageQueueCandidate);
        }
    }
}
