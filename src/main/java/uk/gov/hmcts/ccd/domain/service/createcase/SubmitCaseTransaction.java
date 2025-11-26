package uk.gov.hmcts.ccd.domain.service.createcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.persistence.CasePointerRepository;
import uk.gov.hmcts.ccd.decentralised.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.decentralised.service.DecentralisedCreateCaseEventService;
import uk.gov.hmcts.ccd.decentralised.service.SynchronisedCaseProcessor;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;
import uk.gov.hmcts.ccd.ApplicationParams;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

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
    private final DecentralisedCreateCaseEventService decentralisedSubmitCaseTransaction;
    private final PersistenceStrategyResolver resolver;
    private final CasePointerRepository casePointerRepository;
    private final SynchronisedCaseProcessor synchronisedCaseProcessor;

    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                     final CaseDetailsRepository caseDetailsRepository,
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
                                 final DecentralisedCreateCaseEventService decentralisedSubmitCaseTransaction,
                                 final PersistenceStrategyResolver resolver,
                                 final CasePointerRepository casePointerRepository,
                                 final SynchronisedCaseProcessor synchronisedCaseProcessor
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
        this.decentralisedSubmitCaseTransaction = decentralisedSubmitCaseTransaction;
        this.resolver = resolver;
        this.casePointerRepository = casePointerRepository;
        this.synchronisedCaseProcessor = synchronisedCaseProcessor;
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

        @SuppressWarnings("UnnecessaryLocalVariable")
        final CaseDetails caseDetailsAfterCallback = caseDetailsWithoutHashes;

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

        CaseDetails savedCaseDetails;
        if (resolver.isDecentralised(caseDetailsAfterCallbackWithoutHashes)) {
            // Granting documents & attaching documents must be done once a case reference is allocated but before the
            // decentralised submit call, to align with the behaviour of the centralised submit case transaction.
            savedCaseDetails = submitDecentralisedCase(event, caseTypeDefinition, idamUser, caseEventDefinition,
                onBehalfOfUser, caseDetailsAfterCallbackWithoutHashes, documentHashes);
        } else {
            savedCaseDetails = saveAuditEventForCaseDetails(
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
        }


        return savedCaseDetails;
    }

    private CaseDetails submitDecentralisedCase(Event event, CaseTypeDefinition caseTypeDefinition, IdamUser idamUser,
                                       CaseEventDefinition caseEventDefinition, IdamUser onBehalfOfUser,
                                       CaseDetails newCaseDetails,
                                       List<DocumentHashToken> documentHashes) {
        this.casePointerRepository.persistCasePointerAndInitId(newCaseDetails);

        caseDataAccessControl.grantAccess(newCaseDetails, idamUser.getId());

        caseDocumentService.attachCaseDocuments(
            newCaseDetails.getReferenceAsString(),
            newCaseDetails.getCaseTypeId(),
            newCaseDetails.getJurisdiction(),
            documentHashes
        );

        try {
            DecentralisedCaseDetails decentralisedCaseDetails =
                decentralisedSubmitCaseTransaction.submitDecentralisedEvent(event,
                    caseEventDefinition, caseTypeDefinition, newCaseDetails, Optional.empty(),
                    Optional.ofNullable(onBehalfOfUser));

            synchronisedCaseProcessor.applyConditionallyWithLock(decentralisedCaseDetails, freshDetails ->
                casePointerRepository.updateResolvedTtl(
                    freshDetails.getReference(),
                    newCaseDetails.getResolvedTTL())
            );

            return decentralisedCaseDetails.getCaseDetails();
        } catch (ApiException apiException) {
            // Downstream service rejected the submission (errors or warnings where ignore_warning was false)
            boolean hasErrors = apiException.getCallbackErrors() != null
                && !apiException.getCallbackErrors().isEmpty();
            boolean hasWarnings = apiException.getCallbackWarnings() != null
                && !apiException.getCallbackWarnings().isEmpty();

            log.warn(
                "Decentralised submission for case {} event {} rejected with {} callback error(s) and {} warning(s)",
                newCaseDetails.getReference(),
                event.getEventId(),
                hasErrors ? apiException.getCallbackErrors().size() : 0,
                hasWarnings ? apiException.getCallbackWarnings().size() : 0
            );

            if (hasErrors || hasWarnings) {
                rollbackCasePointer(newCaseDetails.getReference());
            }
            throw apiException;
        } catch (FeignException feignException) {
            // Rollback case pointer if downstream service returns 4xx error
            if (feignException.status() >= 400 && feignException.status() < 500) {
                log.warn("Decentralised submission for case {} event {} failed with HTTP status {}",
                    newCaseDetails.getReference(),
                    event.getEventId(),
                    feignException.status());
                rollbackCasePointer(newCaseDetails.getReference());
            } else {
                log.error("Decentralised submission for case {} event {} failed with HTTP status {}",
                    newCaseDetails.getReference(),
                    event.getEventId(),
                    feignException.status());
            }
            throw feignException;
        }
    }

    private CaseDetails saveAuditEventForCaseDetails(AboutToSubmitCallbackResponse response,
                                                     Event event,
                                                     CaseTypeDefinition caseTypeDefinition,
                                                     IdamUser idamUser,
                                                     CaseEventDefinition caseEventDefinition,
                                                     CaseDetails newCaseDetails,
                                                     IdamUser onBehalfOfUser) {

        CaseDetails savedCaseDetails = caseDetailsRepository.set(newCaseDetails);
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

        messageService.handleMessage(MessageContext.builder()
            .caseDetails(savedCaseDetails)
            .caseTypeDefinition(caseTypeDefinition)
            .caseEventDefinition(caseEventDefinition)
            .oldState(null).build());
        return savedCaseDetails;
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

    private void rollbackCasePointer(Long caseReference) {
        try {
            log.info("Rolling back case pointer for case reference: {}", caseReference);
            casePointerRepository.deleteCasePointer(caseReference);
        } catch (Exception e) {
            // Log the error but don't fail the original operation
            // The client should receive the original error, not this rollback error
            // This is a best-effort cleanup
            log.error("Failed to rollback case pointer for case reference: {}. Error: {}",
                caseReference, e.getMessage(), e);
        }
    }

}
