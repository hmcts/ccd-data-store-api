package uk.gov.hmcts.ccd.domain.service.startevent;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.function.Supplier;

@Service
@Qualifier("default")
public class DefaultStartEventOperation implements StartEventOperation {

    private final EventTokenService eventTokenService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseService caseService;
    private final CaseTypeService caseTypeService;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;

    @Autowired
    public DefaultStartEventOperation(final EventTokenService eventTokenService,
                               @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                               @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                               final EventTriggerService eventTriggerService,
                               final CaseService caseService,
                               final CaseTypeService caseTypeService,
                               final CallbackInvoker callbackInvoker,
                               final UIDService uidService) {

        this.eventTokenService = eventTokenService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseService = caseService;
        this.caseTypeService = caseTypeService;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
    }

    @Override
    public StartEventTrigger triggerStartForCaseType(final Integer uid,
                                                               final String jurisdictionId,
                                                               final String caseTypeId,
                                                               final String eventTriggerId,
                                                               final Boolean ignoreWarning) {

        final CaseType caseType = getCaseType(caseTypeId);
        final CaseEvent eventTrigger = getEventTrigger(caseTypeId, eventTriggerId, caseType);

        validateJurisdiction(jurisdictionId, caseTypeId, caseType);

        final CaseDetails caseDetails = caseService.createNewCaseDetails(caseTypeId, jurisdictionId, Maps.newHashMap());

        validateEventTrigger(() -> !eventTriggerService.isPreStateEmpty(eventTrigger));

        final String eventToken = eventTokenService.generateToken(uid, eventTrigger, caseType.getJurisdiction(), caseType);

        callbackInvoker.invokeAboutToStartCallback(eventTrigger, caseType, caseDetails, ignoreWarning);

        return buildStartEventTrigger(eventTriggerId, eventToken, caseDetails);
    }

    @Override
    public StartEventTrigger triggerStartForCase(final Integer uid,
                                                 final String jurisdictionId,
                                                 final String caseTypeId,
                                                 final String caseReference,
                                                 final String eventTriggerId,
                                                 final Boolean ignoreWarning) {

        final CaseType caseType = getCaseType(caseTypeId);
        final CaseEvent eventTrigger = getEventTrigger(caseTypeId, eventTriggerId, caseType);

        validateJurisdiction(jurisdictionId, caseTypeId, caseType);

        final CaseDetails caseDetails = getCaseDetails(jurisdictionId, caseTypeId, caseReference);

        validateEventTrigger(() -> !eventTriggerService.isPreStateValid(caseDetails.getState(), eventTrigger));

        final String eventToken = eventTokenService.generateToken(uid, caseDetails, eventTrigger,
            caseType.getJurisdiction(), caseType);

        callbackInvoker.invokeAboutToStartCallback(eventTrigger, caseType, caseDetails, ignoreWarning);

        return buildStartEventTrigger(eventTriggerId, eventToken, caseDetails);
    }

    private StartEventTrigger buildStartEventTrigger(String eventTriggerId, String eventToken, CaseDetails caseDetails) {
        final StartEventTrigger startEventTrigger = new StartEventTrigger();
        startEventTrigger.setCaseDetails(caseDetails);
        startEventTrigger.setToken(eventToken);
        startEventTrigger.setEventId(eventTriggerId);
        return startEventTrigger;
    }

    private void validateJurisdiction(String jurisdictionId, String caseTypeId, CaseType caseType) {
        if (!caseTypeService.isJurisdictionValid(jurisdictionId, caseType)) {
            throw new ValidationException(caseTypeId + " is not defined as a case type for " + jurisdictionId);
        }
    }

    private CaseDetails getCaseDetails(String jurisdictionId, String caseTypeId, String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        final CaseDetails caseDetails = caseDetailsRepository.findUniqueCase(jurisdictionId, caseTypeId, caseReference);
        if (caseDetails == null) {
            throw new ResourceNotFoundException("No case exist with id=" + caseReference);
        }
        return caseDetails;
    }

    private CaseEvent getEventTrigger(String caseTypeId, String eventTriggerId, CaseType caseType) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, eventTriggerId);
        if (eventTrigger == null) {
            throw new ResourceNotFoundException("Cannot findCaseEvent event " + eventTriggerId + " for case type " + caseTypeId);
        }
        return eventTrigger;
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ResourceNotFoundException("Cannot findCaseEvent case type definition for  " + caseTypeId);
        }
        return caseType;
    }

    private void validateEventTrigger(Supplier<Boolean> validationOperation) {
        if (validationOperation.get()) {
            throw new ValidationException("The case status did not qualify for the event");
        }
    }

}
