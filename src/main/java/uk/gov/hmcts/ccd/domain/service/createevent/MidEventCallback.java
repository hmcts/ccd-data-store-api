package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class MidEventCallback {

    private final CallbackInvoker callbackInvoker;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseService caseService;

    final JcLogger jcLogger = new JcLogger("MidEventCallback", true);

    @Autowired
    public MidEventCallback(CallbackInvoker callbackInvoker,
                            UIDefinitionRepository uiDefinitionRepository,
                            EventTriggerService eventTriggerService,
                            @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                    CaseDefinitionRepository caseDefinitionRepository,
                            CaseService caseService) {
        this.callbackInvoker = callbackInvoker;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseService = caseService;
    }

    private void logData(final CaseDataContent caseDataContent, final String methodReference) {
        final String json = jcLogger.getObjectAsString(caseDataContent);
        if (json.contains("dummy.pdf")) {
            jcLogger.jclog(methodReference + ": YES , json = " + json);
        } else {
            jcLogger.jclog(methodReference + ": NO , json = " + json);
        }
    }

    private void logData(final CaseDetails caseDetails, final String methodReference) {
        final String json = jcLogger.getObjectAsString(caseDetails);
        if (json.contains("dummy.pdf")) {
            jcLogger.jclog(methodReference + ": YES , json = " + json);
        } else {
            jcLogger.jclog(methodReference + ": NO , json = " + json);
        }
    }

    /*
     * Called directly from CaseDataValidatorController.validate()
     * Method below references 'caseDetailsBefore' , 'currentOrNewCaseDetails' , 'caseDetails' , and
     * 'caseDetailsFromMidEventCallback'
     */
    @Transactional
    public JsonNode invoke(String caseTypeId,
                           CaseDataContent content,
                           String pageId) {
        if (!isBlank(pageId)) {

            // QUESTION: Does 'CaseDataContent content' contain reference to "dummy.pdf" ?
            logData(content, "invoke #1");

            Event event = content.getEvent();
            final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);
            final CaseEventDefinition caseEventDefinition = getCaseEvent(event, caseTypeDefinition);

            Optional<WizardPage> wizardPageOptional = uiDefinitionRepository
                .getWizardPageCollection(caseTypeId, event.getEventId())
                .stream()
                .filter(wizardPage -> wizardPage.getId().equals(pageId))
                .findFirst();

            if (wizardPageOptional.isPresent() && !isBlank(wizardPageOptional.get().getCallBackURLMidEvent())) {

                CaseDetails caseDetailsBefore = null;
                CaseDetails currentOrNewCaseDetails;
                if (StringUtils.isNotEmpty(content.getCaseReference())) {

                    /*
                     * LINE 75 (call caseService.getCaseDetails())
                     */
                    final CaseDetails caseDetails =
                        caseService.getCaseDetails(caseTypeDefinition.getJurisdictionId(), content.getCaseReference());

                    // QUESTION: Does caseDetails contain reference to "dummy.pdf" ?
                    logData(caseDetails, "invoke #2");

                    caseDetailsBefore = caseService.clone(caseDetails);

                    // QUESTION: Does caseDetailsBefore contain reference to "dummy.pdf" ?
                    logData(caseDetailsBefore, "invoke #3");

                    currentOrNewCaseDetails =
                        caseService.populateCurrentCaseDetailsWithEventFields(content, caseDetails);

                    // QUESTION: Does currentOrNewCaseDetails contain reference to "dummy.pdf" ?
                    logData(currentOrNewCaseDetails, "invoke #4");

                } else {
                    currentOrNewCaseDetails =
                        caseService.createNewCaseDetails(caseTypeId, caseTypeDefinition.getJurisdictionId(),
                        content.getEventData() == null ? content.getData() : content.getEventData());
                }
                removeNextPageFieldData(currentOrNewCaseDetails, wizardPageOptional.get().getOrder(), caseTypeId,
                    event.getEventId());

                // QUESTION: Does currentOrNewCaseDetails contain reference to "dummy.pdf" ?
                logData(currentOrNewCaseDetails, "invoke #5");

                /*
                 * LINE 88 (call callbackInvoker.invokeMidEventCallback())
                 */
                CaseDetails caseDetailsFromMidEventCallback =
                    callbackInvoker.invokeMidEventCallback(wizardPageOptional.get(),
                    caseTypeDefinition, caseEventDefinition,
                    caseDetailsBefore,
                    currentOrNewCaseDetails,
                    content.getIgnoreWarning());

                return dataJsonNode(caseDetailsFromMidEventCallback.getData());
            }
        }
        return dataJsonNode(content.getData());
    }

    private void removeNextPageFieldData(CaseDetails currentCaseDetails, Integer order,
                                         String caseTypeId, String eventId) {
        if (order != null) {
            Set<String> wizardPageFields = uiDefinitionRepository
                .getWizardPageCollection(caseTypeId, eventId)
                .stream()
                .filter(wizardPage -> wizardPage.getOrder() > order)
                .map(WizardPage::getWizardPageFieldNames)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            currentCaseDetails.getData().entrySet().removeIf(entry -> wizardPageFields.contains(entry.getKey()));
        }
    }

    private JsonNode dataJsonNode(Map<String, JsonNode> data) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.set("data", mapper.valueToTree(data));
        return objectNode;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    private CaseEventDefinition getCaseEvent(Event event, CaseTypeDefinition caseTypeDefinition) {
        final CaseEventDefinition caseEventDefinition =
            eventTriggerService.findCaseEvent(caseTypeDefinition, event.getEventId());
        if (caseEventDefinition == null) {
            throw new ValidationException(event.getEventId() + " is not a known event ID for the specified case type "
                + caseTypeDefinition.getId());
        }
        return caseEventDefinition;
    }
}
