package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.comparingInt;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Service
public class SecurityClassificationService {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final ObjectNode EMPTY_NODE = JSON_NODE_FACTORY.objectNode();

    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationService.class);

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

    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails) {
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails.getJurisdiction());
        Optional<CaseDetails> result = Optional.of(caseDetails);

        return userClassificationOpt
            .flatMap(securityClassification -> result.filter(caseHasClassificationEqualOrLowerThan(securityClassification))
                .map(cd -> {
                    if (cd.getDataClassification() == null) {
                        LOG.warn("No data classification for case with reference={}, all fields removed", cd.getReference());
                        cd.setDataClassification(Maps.newHashMap());
                    }

                    JsonNode data = filterNestedObject(JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                        JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                        securityClassification);
                    caseDetails.setData(JacksonUtils.convertValue(data));
                    return cd;
                }));
    }

    public List<AuditEvent> applyClassification(String jurisdictionId, List<AuditEvent> events) {
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
        return userClassification.map(securityClassification -> securityClassification.higherOrEqualTo(
            caseType.getClassificationForField(fieldId))).orElse(false);
    }

    private JsonNode filterNestedObject(JsonNode data, JsonNode dataClassification, SecurityClassification userClassification) {
        if (isAnyNull(data, dataClassification)) {
            return EMPTY_NODE;
        }
        Iterator<Map.Entry<String, JsonNode>> dataIterator = data.fields();
        while (dataIterator.hasNext()) {
            Map.Entry<String, JsonNode> dataElement = dataIterator.next();
            String dataElementKey = dataElement.getKey();
            JsonNode dataClassificationElement = dataClassification.get(dataElementKey);
            if (isAnyNull(dataClassificationElement)) {
                dataIterator.remove();
            } else if (dataClassificationElement.has(VALUE)) {
                JsonNode dataClassificationValue = dataClassificationElement.get(VALUE);
                JsonNode dataElementValue = dataElement.getValue();
                if (dataClassificationValue.isObject()) {
                    filterObject(userClassification, dataIterator, dataClassificationElement, dataElementValue);
                } else {
                    filterCollection(userClassification,
                        dataIterator,
                        dataClassificationElement,
                        dataElementValue);
                }
            } else if (dataClassificationElement.isTextual()) {
                filterSimpleField(userClassification, dataIterator, dataClassificationElement);
            } else {
                dataIterator.remove();
            }
        }
        return data;
    }

    private void filterCollection(SecurityClassification userClassification,
                                  Iterator<Map.Entry<String, JsonNode>> dataIterator,
                                  JsonNode dataClassificationElement, JsonNode dataElementValue) {
        // Apply collection-level classification
        filterSimpleField(userClassification,
            dataIterator,
            dataClassificationElement.get(CLASSIFICATION));

        Iterator<JsonNode> dataCollectionIterator = dataElementValue.iterator();
        while (dataCollectionIterator.hasNext()) {
            JsonNode collectionElement = dataCollectionIterator.next();
            JsonNode dataClassificationForData = getDataClassificationForData(collectionElement,
                dataClassificationElement.get(VALUE).iterator());
            if (dataClassificationForData.isNull()) {
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
                } else {
                    LOG.warn("Invalid security classification structure for collection item: {}", relevantDataClassificationValue.toString());
                    dataCollectionIterator.remove();
                }
            } else {
                // For collection of simple field type, the classification is stored as `classification`, not `value`
                relevantDataClassificationValue = dataClassificationForData.get(CLASSIFICATION);

                if (null != relevantDataClassificationValue) {
                    filterSimpleField(userClassification,
                        dataCollectionIterator,
                        relevantDataClassificationValue);
                } else {
                    dataCollectionIterator.remove();
                }
            }
            if (collectionElementValue.equals(EMPTY_NODE)) {
                dataCollectionIterator.remove();
            }
        }
    }

    private void filterObject(SecurityClassification userClassification,
                              Iterator<Map.Entry<String, JsonNode>> dataIterator,
                              JsonNode dataClassificationParent, JsonNode dataElementValue) {
        filterNestedObject(dataElementValue,
            dataClassificationParent.get(VALUE),
            userClassification);
        if (dataElementValue.equals(EMPTY_NODE)) {
            filterSimpleField(userClassification,
                dataIterator,
                dataClassificationParent.get(CLASSIFICATION));
        }
    }

    private void filterSimpleField(SecurityClassification userClassification, Iterator iterator, JsonNode dataClassificationValue) {
        Optional<SecurityClassification> securityClassification = getSecurityClassification(dataClassificationValue);
        if (!securityClassification.isPresent() || !userClassification.higherOrEqualTo(securityClassification.get())) {
            iterator.remove();
        }
    }

    private boolean isAnyNull(JsonNode... jsonNodes) {
        return newArrayList(jsonNodes).stream().anyMatch(Objects::isNull);
    }
}
