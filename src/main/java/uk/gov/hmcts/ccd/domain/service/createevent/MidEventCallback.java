package uk.gov.hmcts.ccd.domain.service.createevent;

import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

@Service
public class MidEventCallback {

    private final CallbackInvoker callbackInvoker;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseService caseService;

    @Autowired
    public MidEventCallback(CallbackInvoker callbackInvoker,
                            UIDefinitionRepository uiDefinitionRepository,
                            EventTriggerService eventTriggerService,
                            @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
                            CaseService caseService) {
        this.callbackInvoker = callbackInvoker;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseService = caseService;
    }

    public JsonNode invoke(String caseTypeId,
                           CaseDataContent content,
                           String pageId) {
        if (!isBlank(pageId)) {
            Event event = content.getEvent();
            final CaseType caseType = getCaseType(caseTypeId);
            final CaseEvent caseEvent = getCaseEvent(event, caseType);

            Optional<WizardPage> wizardPageOptional = uiDefinitionRepository
                .getWizardPageCollection(caseTypeId, event.getEventId())
                .stream()
                .filter(wizardPage -> wizardPage.getId().equals(pageId))
                .findFirst();

            if (wizardPageOptional.isPresent() && !isBlank(wizardPageOptional.get().getCallBackURLMidEvent())) {

                CaseDetails caseDetailsBefore = null;
                CaseDetails currentOrNewCaseDetails;
                if (StringUtils.isNotEmpty(content.getCaseReference())) {
                    CaseDetails caseDetails = caseService.getCaseDetails(caseType.getJurisdictionId(), content.getCaseReference());
                    caseDetailsBefore = caseService.clone(caseDetails);
                    currentOrNewCaseDetails = caseService.populateCurrentCaseDetailsWithEventFields(content,
                        caseDetails);

                } else {
                    currentOrNewCaseDetails = caseService.createNewCaseDetails(caseTypeId, caseType.getJurisdictionId(),
                        content.getEventData() == null ? content.getData() : content.getEventData());
                }

                CaseDetails caseDetailsFromMidEventCallback = callbackInvoker.invokeMidEventCallback(wizardPageOptional.get(),
                    caseType,
                    caseEvent,
                    caseDetailsBefore,
                    currentOrNewCaseDetails,
                    content.getIgnoreWarning());

                return dataJsonNode(caseDetailsFromMidEventCallback.getData());
            }
        }
        return dataJsonNode(content.getData());
    }

    private JsonNode dataJsonNode(Map<String, JsonNode> data) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.set("data", mapper.valueToTree(data));
        return objectNode;
    }

    private CaseType getCaseType(String caseTypeId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        return caseType;
    }

    private CaseEvent getCaseEvent(Event event, CaseType caseType) {
        final CaseEvent caseEvent = eventTriggerService.findCaseEvent(caseType, event.getEventId());
        if (caseEvent == null) {
            throw new ValidationException(event.getEventId() + " is not a known event ID for the specified case type " + caseType.getId());
        }
        return caseEvent;
    }
}
