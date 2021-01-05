package uk.gov.hmcts.ccd.domain.service.message;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;

import java.util.List;

public abstract class AbstractMessageService implements MessageService {
    private final UserRepository userRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;

    protected AbstractMessageService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                     CaseAuditEventRepository caseAuditEventRepository) {
        this.userRepository = userRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
    }

    MessageInformation populateMessageInformation(MessageContext messageContext) {

        final MessageInformation messageInformation = new MessageInformation();
        final IdamUser user = userRepository.getUser();
        List<AuditEvent> auditEvent = caseAuditEventRepository.findByCase(messageContext.getCaseDetails());

        messageInformation.setCaseId(messageContext.getCaseDetails().getReference().toString());
        messageInformation.setJurisdictionId(messageContext.getCaseDetails().getJurisdiction());
        messageInformation.setCaseTypeId(messageContext.getCaseDetails().getCaseTypeId());
        messageInformation.setEventInstanceId(auditEvent.get(0).getId());
        messageInformation.setEventTimestamp(auditEvent.get(0).getCreatedDate());
        messageInformation.setEventId(messageContext.getCaseEventDefinition().getId());
        messageInformation.setUserId(user.getId());
        messageInformation.setPreviousStateId(messageContext.getOldState());
        messageInformation.setNewStateId(messageContext.getCaseDetails().getState());

        return messageInformation;
    }
}
