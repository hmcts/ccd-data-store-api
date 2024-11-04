package uk.gov.hmcts.ccd.domain.service.createcase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

@Slf4j
@Service
public class SubmitCaseTransaction implements AccessControl {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;
    private final SecurityClassificationService securityClassificationService;
    private final CaseDataAccessControl caseDataAccessControl;
    private final MessageService messageService;
    private final CaseDocumentService caseDocumentService;
    private final ApplicationParams applicationParams;
    private final CaseAccessGroupUtils caseAccessGroupUtils;
    private final CaseDocumentTimestampService caseDocumentTimestampService;
    private final POCSubmitCaseTransaction pocSubmitCaseTransaction;

    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                 final CaseAuditEventRepository caseAuditEventRepository,
                                 final CaseTypeService caseTypeService,
                                 final CallbackInvoker callbackInvoker,
                                 final UIDService uidService,
                                 final SecurityClassificationService securityClassificationService,
                                 final CaseDataAccessControl caseDataAccessControl,
                                 final @Qualifier("caseEventMessageService") MessageService messageService,
                                 final CaseDocumentService caseDocumentService,
                                 final ApplicationParams applicationParams,
                                 final CaseAccessGroupUtils caseAccessGroupUtils,
                                 final CaseDocumentTimestampService caseDocumentTimestampService,
                                 final POCSubmitCaseTransaction pocSubmitCaseTransaction
    ) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.caseTypeService = caseTypeService;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.caseDataAccessControl = caseDataAccessControl;
        this.messageService = messageService;
        this.caseDocumentService = caseDocumentService;
        this.applicationParams = applicationParams;
        this.caseAccessGroupUtils = caseAccessGroupUtils;
        this.caseDocumentTimestampService = caseDocumentTimestampService;
        this.pocSubmitCaseTransaction = pocSubmitCaseTransaction;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            value = {ReferenceKeyUniqueConstraintException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 50)
    )

    public CaseDetails submitCase(Event event,
                                  CaseTypeDefinition caseTypeDefinition,
                                  IdamUser idamUser,
                                  CaseEventDefinition caseEventDefinition,
                                  CaseDetails caseDetails,
                                  Boolean ignoreWarning,
                                  IdamUser onBehalfOfUser) {

        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        caseDetails.setCreatedDate(now);
        caseDetails.setLastStateModifiedDate(now);
        caseDetails.setReference(Long.valueOf(uidService.generateUID()));

        final CaseDetails caseDetailsWithoutHashes = caseDocumentService.stripDocumentHashes(caseDetails);

        /*
            About to submit

            TODO: Ideally, the callback should be outside of the transaction. However, it requires the case UID to have
            been assigned and the UID generation has to be part of a retryable transaction in order to recover from
            collisions.
         */
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition,
                null,
                caseDetailsWithoutHashes,
                caseTypeDefinition,
                ignoreWarning
        );

        caseDocumentTimestampService.addUploadTimestamps(caseDetailsWithoutHashes, null);

        @SuppressWarnings("UnnecessaryLocalVariable") final CaseDetails caseDetailsAfterCallback = caseDetailsWithoutHashes;

        final List<DocumentHashToken> documentHashes = caseDocumentService.extractDocumentHashToken(
                caseDetails.getData(),
                caseDetailsAfterCallback.getData()
        );

        final CaseDetails caseDetailsAfterCallbackWithoutHashes = caseDocumentService.stripDocumentHashes(
                caseDetailsAfterCallback
        );

        if (this.applicationParams.getCaseGroupAccessFilteringEnabled()) {
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetailsAfterCallbackWithoutHashes,
                    caseTypeDefinition);
        }

        final CaseDetails savedCaseDetails = saveAuditEventForCaseDetails(
                aboutToSubmitCallbackResponse,
                event,
                caseTypeDefinition,
                idamUser,
                caseEventDefinition,
                caseDetailsAfterCallbackWithoutHashes,
                onBehalfOfUser
        );

        caseDataAccessControl.grantAccess(savedCaseDetails, idamUser.getId());

        caseDocumentService.attachCaseDocuments(
                caseDetails.getReferenceAsString(),
                caseDetails.getCaseTypeId(),
                caseDetails.getJurisdiction(),
                documentHashes
        );

        return savedCaseDetails;
    }

    private CaseDetails saveAuditEventForCaseDetails(AboutToSubmitCallbackResponse response,
                                                     Event event,
                                                     CaseTypeDefinition caseTypeDefinition,
                                                     IdamUser idamUser,
                                                     CaseEventDefinition caseEventDefinition,
                                                     CaseDetails newCaseDetails,
                                                     IdamUser onBehalfOfUser) {

        final CaseDetails pocCaseDetails = pocSubmitCaseTransaction.saveAuditEventForCaseDetails(response,
                event, caseTypeDefinition, idamUser, caseEventDefinition, newCaseDetails, onBehalfOfUser);
        final CaseDetails savedCaseDetails = caseDetailsRepository.set(newCaseDetails);
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(caseEventDefinition.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(savedCaseDetails.getId());
        auditEvent.setData(savedCaseDetails.getData());
        auditEvent.setStateId(savedCaseDetails.getState());
        CaseStateDefinition caseStateDefinition =
                caseTypeService.findState(caseTypeDefinition, savedCaseDetails.getState());
        auditEvent.setStateName(caseStateDefinition.getName());
        auditEvent.setCaseTypeId(caseTypeDefinition.getId());
        auditEvent.setCaseTypeVersion(caseTypeDefinition.getVersion().getNumber());
        auditEvent.setCreatedDate(newCaseDetails.getCreatedDate());
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseTypeDefinition,
                caseEventDefinition));
        auditEvent.setDataClassification(savedCaseDetails.getDataClassification());
        auditEvent.setSignificantItem(response.getSignificantItem());
        saveUserDetails(idamUser, onBehalfOfUser, auditEvent);

        caseAuditEventRepository.set(auditEvent);

        CaseDetails messageCaseDetails
                = this.applicationParams.getPocCaseTypes().contains(savedCaseDetails.getCaseTypeId())
                ? pocCaseDetails : savedCaseDetails;

        messageService.handleMessage(MessageContext.builder()
                .caseDetails(messageCaseDetails)
                .caseTypeDefinition(caseTypeDefinition)
                .caseEventDefinition(caseEventDefinition)
                .oldState(null).build());
        return messageCaseDetails;
    }

    private void saveUserDetails(IdamUser idamUser, IdamUser onBehalfOfUser, AuditEvent auditEvent) {
        if (onBehalfOfUser == null) {
            auditEvent.setUserId(idamUser.getId());
            auditEvent.setUserLastName(idamUser.getSurname());
            auditEvent.setUserFirstName(idamUser.getForename());
        } else {
            auditEvent.setUserId(onBehalfOfUser.getId());
            auditEvent.setUserLastName(onBehalfOfUser.getSurname());
            auditEvent.setUserFirstName(onBehalfOfUser.getForename());
            auditEvent.setProxiedBy(idamUser.getId());
            auditEvent.setProxiedByLastName(idamUser.getSurname());
            auditEvent.setProxiedByFirstName(idamUser.getForename());
        }
    }

}
