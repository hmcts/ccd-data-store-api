package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.*;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.comparingInt;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.valueOf;

@Service
public class SecurityClassificationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String ID = "id";
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final ObjectNode EMPTY_NODE = JSON_NODE_FACTORY.objectNode();
    private static final ArrayNode EMPTY_ARRAY = JSON_NODE_FACTORY.arrayNode();

    private static final Logger LOG = LogManager.getLogger(SecurityClassificationService.class);

    private final UserRepository userRepository;

    @Autowired
    public SecurityClassificationService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<SecurityClassification> getUserClassification(String jurisdictionId) {
        return userRepository.getUserClassifications(jurisdictionId)
                             .stream()
                             .max(comparingInt(SecurityClassification::getRank));
    }

    public Optional<CaseDetails> apply(CaseDetails caseDetails) {
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails.getJurisdiction());
        Optional<CaseDetails> result = Optional.of(caseDetails);

        return result.filter(caseIfHasClassificationEqualOrLowerThan(userClassificationOpt))
                .map(cd -> {
                    if (cd.getDataClassification() == null) {
                        LOG.warn("No data classification for case with reference={}, all fields removed", cd.getReference());
                        cd.setDataClassification(Maps.newHashMap());
                    }

                    JsonNode data = filterNestedObject(MAPPER.convertValue(caseDetails.getData(), JsonNode.class),
                            MAPPER.convertValue(caseDetails.getDataClassification(), JsonNode.class),
                            userClassificationOpt.get());
                    caseDetails.setData(MAPPER.convertValue(data, STRING_JSON_MAP));
                    return cd;
                });
    }

    public List<AuditEvent> apply(String jurisdictionId, List<AuditEvent> events) {
        final Optional<SecurityClassification> userClassification = getUserClassification(jurisdictionId);

        if (null == events || !userClassification.isPresent()) {
            return newArrayList();
        }

        final ArrayList<AuditEvent> classifiedEvents = newArrayList();

        for (AuditEvent event : events) {
            if (userClassification.get().higherOrEqualTo(event.getSecurityClassification())) {
                classifiedEvents.add(event);
            }
        }

        return classifiedEvents;
    }

    public SecurityClassification getClassificationForEvent(CaseType caseType, CaseEvent eventTrigger) {
        return caseType
            .getEvents()
            .stream()
            .filter(e -> e.getId().equals(eventTrigger.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("EventId %s not found", eventTrigger.getId())))
            .getSecurityClassification();
    }

    public boolean userHasEnoughSecurityClassificationForField(String jurisdictionId, CaseType caseType, String fieldId) {
        final Optional<SecurityClassification> userClassification = getUserClassification(jurisdictionId);
        return userClassification.map(securityClassification -> securityClassification.higherOrEqualTo
            (caseType.getClassificationForField(fieldId))).orElse(false);
    }

    private JsonNode filterNestedObject(JsonNode data, JsonNode dataClassification, SecurityClassification userClassification) {
        if (isNull(data, dataClassification)) {
            return EMPTY_NODE;
        }
        Iterator<Map.Entry<String, JsonNode>> dataIterator = data.fields();
        while (dataIterator.hasNext()) {
            Map.Entry<String, JsonNode> dataElement = dataIterator.next();
            String dataElementKey = dataElement.getKey();
            JsonNode dataClassificationParent = dataClassification.get(dataElementKey);
            if (isNull(dataClassificationParent)) {
                dataIterator.remove();
            } else if (dataClassificationParent.has(VALUE)) {
                JsonNode dataClassificationValue = dataClassificationParent.get(VALUE);
                JsonNode dataElementValue = dataElement.getValue();
                if (dataClassificationValue.isObject()) {
                    filterObject(userClassification, dataIterator, dataClassificationParent, dataElementValue);
                } else if (dataClassificationValue.isArray()) {
                    filterCollection(userClassification,
                                     dataIterator,
                                     dataClassificationParent,
                                     dataClassificationValue,
                                     dataElementValue);
                }
            } else if (dataClassificationParent.isTextual()) {
                filterSimpleField(userClassification, dataIterator, dataClassificationParent);
            }
        }
        return data;
    }

    private void filterCollection(SecurityClassification userClassification, Iterator<Map.Entry<String, JsonNode>> dataIterator, JsonNode dataClassificationParent, JsonNode dataClassificationValue, JsonNode dataElementValue) {
        Iterator<JsonNode> dataCollectionIterator = dataElementValue.iterator();
        while (dataCollectionIterator.hasNext()) {
            JsonNode collectionElement = dataCollectionIterator.next();
            JsonNode dataClassificationForData = getDataClassificationForData(collectionElement,
                                                                              dataClassificationValue.iterator());
            if (dataClassificationForData == null) {
                dataCollectionIterator.remove();
                continue;
            }
            JsonNode relevantDataClassificationValue = dataClassificationForData.get(VALUE);
            JsonNode collectionElementValue = collectionElement.get(VALUE);
            if (relevantDataClassificationValue != null) {
                if (collectionElementValue.isObject()) {
                    filterNestedObject(collectionElementValue,
                                       relevantDataClassificationValue,
                                       userClassification);
                } else if (collectionElementValue.isTextual()) {
                    filterSimpleField(userClassification,
                                      dataCollectionIterator,
                                      relevantDataClassificationValue.get(collectionElementValue.textValue()));
                }
            }
            if (collectionElementValue.equals(EMPTY_NODE)) {
                dataCollectionIterator.remove();
            }
        }
        if (dataElementValue.equals(EMPTY_ARRAY)) {
            filterSimpleField(userClassification,
                              dataIterator,
                              dataClassificationParent.get(CLASSIFICATION));
        }
    }

    private void filterObject(SecurityClassification userClassification, Iterator<Map.Entry<String, JsonNode>> dataIterator, JsonNode dataClassificationParent, JsonNode dataElementValue) {
        filterNestedObject(dataElementValue,
                           dataClassificationParent.get(VALUE),
                           userClassification);
        if (dataElementValue.equals(EMPTY_NODE)) {
            filterSimpleField(userClassification,
                              dataIterator,
                              dataClassificationParent.get(CLASSIFICATION));
        }
    }

    private boolean isNull(JsonNode... jsonNodes) {
        return newArrayList(jsonNodes).stream().anyMatch(Objects::isNull);
    }

    private JsonNode getDataClassificationForData(JsonNode data, Iterator<JsonNode> dataIterator) {
        Boolean found = false;
        JsonNode dataClassificationValue = null;
        while (dataIterator.hasNext() && !found) {
            dataClassificationValue = dataIterator.next();
            if (null != dataClassificationValue.get(ID)
                    && dataClassificationValue.get(ID).equals(data.get(ID))) {
                found = true;
            }
        }
        return dataClassificationValue;
    }

    private void filterSimpleField(SecurityClassification userClassification, Iterator iterator, JsonNode dataClassificationValue) {
        Optional<SecurityClassification> securityClassification = getSecurityClassification(dataClassificationValue);
        if (!securityClassification.isPresent() || !userClassification.higherOrEqualTo(securityClassification.get())) {
            iterator.remove();
        }
    }

    private Optional<SecurityClassification> getSecurityClassification(JsonNode dataNode) {
        if (dataNode == null || dataNode.isNull()) {
            return Optional.empty();
        }
        SecurityClassification securityClassification;
        try {
            securityClassification = valueOf(dataNode.textValue());
        }
        catch (IllegalArgumentException e) {
            LOG.error("Unable to parse security classification for {}", dataNode, e);
            return Optional.empty();
        }
        return Optional.of(securityClassification);
    }

    private Predicate<CaseDetails> caseIfHasClassificationEqualOrLowerThan(Optional<SecurityClassification> userClassificationOpt) {
        return cd -> userClassificationOpt.map(uc -> uc.higherOrEqualTo(cd.getSecurityClassification())).orElse(false);
    }
}
