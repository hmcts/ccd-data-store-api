package uk.gov.hmcts.ccd.domain.service.createevent;

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
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.DYNAMIC_LIST;

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
        Map<String, JsonNode> data = null;
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

                data = caseDetailsFromMidEventCallback.getData();
            }
        }
        final Map<String, JsonNode> finalData = data != null ? data : content.getData();
        if (content.getEventData() != null) {
            final CaseType caseType = getCaseType(caseTypeId);
            Map<String, JsonNode> dynamicListFields = getDynamicListFieldsIfAlreadyPresent(caseType, content, finalData);
            finalData.putAll(dynamicListFields);
        }
        return dataJsonNode(finalData);
    }

    private Map<String, JsonNode> getDynamicListFieldsIfAlreadyPresent(final CaseType caseType, final CaseDataContent content, final Map<String, JsonNode> finalData) {
        // FE needs to have any dynamic list field value that is coming from previous pages (not this mid event callback) properly formatted i.e.
        // {value: {code:'xyz',label:'XYZ'}, list_items: [{code:'xyz',label:'XYZ'},{code:'abc',label:'ABC'}]}
        Map<String, JsonNode> dynamicListFields = null;
        if (content.getEventData() != null) {
            List<CaseField> caseFieldDefinitions = caseType.getCaseFields();
            dynamicListFields =
                content.getEventData().entrySet().stream()
                    .filter(caseEventDataPair -> !finalData.containsKey(caseEventDataPair.getKey()))
                    .filter(caseEventDataPair -> isFieldOfDynamicListType(caseEventDataPair, caseFieldDefinitions))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }
        return dynamicListFields;
    }

    private boolean isFieldOfDynamicListType(final Map.Entry<String, JsonNode> caseEventDataPair, final List<CaseField> caseFieldDefinition) {
        return caseFieldDefinition.stream()
            .anyMatch(caseField -> caseField.getId().equalsIgnoreCase(caseEventDataPair.getKey())
                && caseField.getFieldType().getType().equals(DYNAMIC_LIST));
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
