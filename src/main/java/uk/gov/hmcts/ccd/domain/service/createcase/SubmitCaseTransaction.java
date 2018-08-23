package uk.gov.hmcts.ccd.domain.service.createcase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
class SubmitCaseTransaction {

    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;
    private final SecurityClassificationService securityClassificationService;
    private final CaseUserRepository caseUserRepository;

    @Inject
    public SubmitCaseTransaction(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                 final CaseAuditEventRepository caseAuditEventRepository,
                                 final CaseTypeService caseTypeService,
                                 final CallbackInvoker callbackInvoker,
                                 final UIDService uidService,
                                 final SecurityClassificationService securityClassificationService,
                                 final CaseUserRepository caseUserRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.caseTypeService = caseTypeService;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.caseUserRepository = caseUserRepository;
    }

    @Transactional
    @Retryable(
        value = {CaseConcurrencyException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 50)
    )
    public CaseDetails submitCase(Event event,
                                  CaseType caseType,
                                  IDAMProperties idamUser,
                                  CaseEvent eventTrigger,
                                  CaseDetails newCaseDetails, Boolean ignoreWarning) {
        final LocalDateTime createdDate = LocalDateTime.now(ZoneOffset.UTC);

        newCaseDetails.setCreatedDate(createdDate);
        newCaseDetails.setReference(Long.valueOf(uidService.generateUID()));

        /*
            About to submit

            TODO: Ideally, the callback should be outside of the transaction. However, it requires the case UID to have
            been assigned and the UID generation has to be part of a retryable transaction in order to recover from collisions.
         */
        callbackInvoker.invokeAboutToSubmitCallback(eventTrigger, null, newCaseDetails, caseType, ignoreWarning);

        final CaseDetails savedCaseDetails = saveAuditEventForCaseDetails(event, caseType, idamUser, eventTrigger, newCaseDetails);

        caseUserRepository.grantAccess(Long.valueOf(savedCaseDetails.getId()), idamUser.getId());

        return savedCaseDetails;
    }

    private CaseDetails saveAuditEventForCaseDetails(Event event, CaseType caseType, IDAMProperties idamUser, CaseEvent eventTrigger, CaseDetails newCaseDetails) {
        final CaseDetails savedCaseDetails = caseDetailsRepository.set(newCaseDetails);
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(eventTrigger.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(savedCaseDetails.getId());
        auditEvent.setData(savedCaseDetails.getData());
        auditEvent.setStateId(savedCaseDetails.getState());
        CaseState caseState = caseTypeService.findState(caseType, savedCaseDetails.getState());
        auditEvent.setStateName(caseState.getName());
        auditEvent.setCaseTypeId(caseType.getId());
        auditEvent.setCaseTypeVersion(caseType.getVersion().getNumber());
        auditEvent.setUserId(idamUser.getId());
        auditEvent.setUserLastName(idamUser.getSurname());
        auditEvent.setUserFirstName(idamUser.getForename());
        auditEvent.setCreatedDate(newCaseDetails.getCreatedDate());
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseType,
                                                                                                     eventTrigger));
        auditEvent.setDataClassification(savedCaseDetails.getDataClassification());
        caseAuditEventRepository.set(auditEvent);
        return savedCaseDetails;
    }

}
