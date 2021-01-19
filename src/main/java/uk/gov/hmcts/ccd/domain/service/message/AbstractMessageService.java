package uk.gov.hmcts.ccd.domain.service.message;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.std.AdditionalMessageInformation;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.MessageInformation;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.AdditionalDataContext;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DefinitionBlockGenerator;
import uk.gov.hmcts.ccd.domain.service.message.additionaldata.DataBlockGenerator;

import java.util.List;

public abstract class AbstractMessageService implements MessageService {
    private final UserRepository userRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final DefinitionBlockGenerator definitionBlockGenerator;
    private final DataBlockGenerator dataBlockGenerator;

    protected AbstractMessageService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                     CaseAuditEventRepository caseAuditEventRepository,
                                     DefinitionBlockGenerator definitionBlockGenerator,
                                     DataBlockGenerator dataBlockGenerator) {
        this.userRepository = userRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.definitionBlockGenerator = definitionBlockGenerator;
        this.dataBlockGenerator = dataBlockGenerator;
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

        AdditionalMessageInformation additionalMessageInformation = new AdditionalMessageInformation();
        additionalMessageInformation.setData(dataBlockGenerator
            .generateData(new AdditionalDataContext(messageContext)));
        additionalMessageInformation.setDefinition(definitionBlockGenerator
            .generateDefinition(new AdditionalDataContext(messageContext)));
        messageInformation.setData(additionalMessageInformation);

        return messageInformation;
    }
}
