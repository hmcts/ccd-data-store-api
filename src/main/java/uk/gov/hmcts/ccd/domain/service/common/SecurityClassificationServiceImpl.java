package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.comparingInt;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getDataClassificationForData;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.getSecurityClassification;

@Service
public class SecurityClassificationServiceImpl implements SecurityClassificationService {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String VALUE = "value";
    private static final String CLASSIFICATION = "classification";
    private static final ObjectNode EMPTY_NODE = JSON_NODE_FACTORY.objectNode();

    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationServiceImpl.class);

    private final CaseDataAccessControl caseDataAccessControl;
    private final CaseDefinitionRepository caseDefinitionRepository;

    final JcLogger jcLogger = new JcLogger("SecurityClassificationServiceImpl");

    @Autowired
    public SecurityClassificationServiceImpl(CaseDataAccessControl caseDataAccessControl,
                                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                             final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDataAccessControl = caseDataAccessControl;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    private Function<CaseDetails, CaseDetails> mapFunction(final CaseDetails caseDetails,
                                                           final SecurityClassification securityClassification) {
        return cd -> {
            if (cd.getDataClassification() == null) {
                LOG.warn("No data classification for case with reference={},"
                    + " all fields removed", cd.getReference());
                jcLogger.jclog("No data classification for case with reference " + cd.getReference());
                cd.setDataClassification(Maps.newHashMap());
            }

            JsonNode data = filterNestedObject(JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                securityClassification);
            caseDetails.setData(JacksonUtils.convertValue(data));
            return cd;
        };
    }

    public Optional<CaseDetails> applyClassification(final CaseDetails caseDetails) {
        return applyClassification(caseDetails, false);
    }

    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails, boolean create) {
        jcLogger.jclog("applyClassification (NORMAL case)", caseDetails);
        jcLogger.jclog("applyClassification (NORMAL case)", caseDetails.getSecurityClassification());
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, create);
        Optional<CaseDetails> caseDetails1 = userClassificationOpt
            .flatMap(securityClassification ->
                Optional.of(caseDetails).filter(caseHasClassificationEqualOrLowerThan(securityClassification))
                    .map(mapFunction(caseDetails, securityClassification)));
        jcLogger.jclog("applyClassification (NORMAL case)", caseDetails1);
        try {
            jcLogger.jclog("applyClassification (NORMAL case)" + (caseDetails1.isPresent()
                ? caseDetails1.get().getSecurityClassification().toString() : "NOT PRESENT"));
        } catch (Exception e) {
            jcLogger.jclog("applyClassification (NORMAL case) ERROR GETTING SECURITY CLASSIFICATION");
        }
        return caseDetails1;
    }

    public List<AuditEvent> applyClassification(CaseDetails caseDetails, List<AuditEvent> events) {
        jcLogger.jclog("applyClassification (AuditEvent)");
        final Optional<SecurityClassification> userClassification = getUserClassification(caseDetails, false);

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

    public Optional<CaseDetails> applyClassificationToRestrictedCase(CaseDetails caseDetails) {
        jcLogger.jclog("applyClassification (RESTRICTED case)", caseDetails);
        jcLogger.jclog("applyClassification (RESTRICTED case)", caseDetails.getSecurityClassification());
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, false);
        Optional<CaseDetails> caseDetails1 = userClassificationOpt
            .flatMap(securityClassification ->
                Optional.of(caseDetails)
                    .map(mapFunction(caseDetails, securityClassification)));
        jcLogger.jclog("applyClassification (RESTRICTED case)", caseDetails1);
        try {
            jcLogger.jclog("applyClassification (RESTRICTED case)" + (caseDetails1.isPresent()
                ? caseDetails1.get().getSecurityClassification().toString() : "NOT PRESENT"));
        } catch (Exception e) {
            jcLogger.jclog("applyClassification (RESTRICTED case) ERROR GETTING SECURITY CLASSIFICATION");
        }
        return caseDetails1;
    }

    public SecurityClassification getClassificationForEvent(CaseTypeDefinition caseTypeDefinition,
                                                            CaseEventDefinition caseEventDefinition) {
        jcLogger.jclog("getClassificationForEvent()");
        return caseTypeDefinition
            .getEvents()
            .stream()
            .filter(e -> e.getId().equals(caseEventDefinition.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("EventId %s not found", caseEventDefinition.getId())))
            .getSecurityClassification();
    }

    public boolean userHasEnoughSecurityClassificationForField(String jurisdictionId,
                                                               CaseTypeDefinition caseTypeDefinition,
                                                               String fieldId) {
        jcLogger.jclog("userHasEnoughSecurityClassificationForField()");
        final Optional<SecurityClassification> userClassification =
            getUserClassification(caseTypeDefinition, false);
        boolean b = userClassification.map(securityClassification ->
            securityClassification.higherOrEqualTo(caseTypeDefinition.getClassificationForField(fieldId)))
            .orElse(false);
        jcLogger.jclog("userHasEnoughSecurityClassificationForField() " + b);
        return b;
    }

    public boolean userHasEnoughSecurityClassificationForField(CaseTypeDefinition caseTypeDefinition,
                                                               SecurityClassification otherClassification) {
        jcLogger.jclog("userHasEnoughSecurityClassificationForField()");
        final Optional<SecurityClassification> userClassification = getUserClassification(caseTypeDefinition, false);
        boolean b = userClassification.map(securityClassification ->
            securityClassification.higherOrEqualTo(otherClassification))
            .orElse(false);
        jcLogger.jclog("userHasEnoughSecurityClassificationForField() " + b);
        return b;
    }

    public Optional<SecurityClassification> getUserClassification(CaseTypeDefinition caseTypeDefinition,
                                                                  boolean isCreateProfile) {
        jcLogger.jclog("getUserClassification()");
        return maxSecurityClassification(caseDataAccessControl
            .getUserClassifications(caseTypeDefinition, isCreateProfile));
    }

    @Override
    public Optional<SecurityClassification> getUserClassification(CaseDetails caseDetails, boolean create) {
        jcLogger.jclog("getUserClassification()");
        if (create) {
            return maxSecurityClassification(caseDataAccessControl.getUserClassifications(
                caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId()), true));
        }
        return maxSecurityClassification(caseDataAccessControl.getUserClassifications(caseDetails));
    }

    private Optional<SecurityClassification> maxSecurityClassification(Set<SecurityClassification> classifications) {
        jcLogger.jclog("maxSecurityClassification()");
        return classifications.stream()
            .filter(classification -> classification != null)
            .max(comparingInt(SecurityClassification::getRank));
    }

    private JsonNode filterNestedObject(JsonNode data, JsonNode dataClassification,
                                        SecurityClassification userClassification) {
        jcLogger.jclog("filterNestedObject()");
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
                                  JsonNode dataClassificationElement,
                                  JsonNode dataElementValue) {
        jcLogger.jclog("filterCollection()");
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
                    LOG.warn("Invalid security classification structure for collection item: {}",
                        relevantDataClassificationValue.toString());
                    jcLogger.jclog("Invalid security classification structure for collection item: "
                        + relevantDataClassificationValue.toString());
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
                              JsonNode dataClassificationParent,
                              JsonNode dataElementValue) {
        jcLogger.jclog("filterObject()");
        filterNestedObject(dataElementValue,
            dataClassificationParent.get(VALUE),
            userClassification);
        if (dataElementValue.equals(EMPTY_NODE)) {
            filterSimpleField(userClassification,
                dataIterator,
                dataClassificationParent.get(CLASSIFICATION));
        }
    }

    private void filterSimpleField(SecurityClassification userClassification, Iterator iterator,
                                   JsonNode dataClassificationValue) {
        jcLogger.jclog("filterSimpleField()");
        Optional<SecurityClassification> securityClassification = getSecurityClassification(dataClassificationValue);
        if (!securityClassification.isPresent() || !userClassification.higherOrEqualTo(securityClassification.get())) {
            iterator.remove();
        }
    }

    private boolean isAnyNull(JsonNode... jsonNodes) {
        return newArrayList(jsonNodes).stream().anyMatch(Objects::isNull);
    }
}
