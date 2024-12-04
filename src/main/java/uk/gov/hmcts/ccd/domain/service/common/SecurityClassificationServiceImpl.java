package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SecurityClassificationServiceImpl(CaseDataAccessControl caseDataAccessControl,
                                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                             final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDataAccessControl = caseDataAccessControl;
        this.caseDefinitionRepository = caseDefinitionRepository;

        // Enables deserialisation of java.util.Optional and java.time.LocalDateTime
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    private void jclog(String message) {
        LOG.info("JCDEBUG: SecurityClassificationServiceImpl: info: {}", message);
    }


    /*
     * MODIFIED "version 1" applyClassification(CaseDetails caseDetails, boolean create).
     * ********
     */
    public Optional<CaseDetails> applyClassificationModifiedVersion1(CaseDetails caseDetails, boolean create) {
        jclog("applyClassification (MODIFIED version 1)");
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, create);
        // TODO: Log userClassificationOpt

        Function<SecurityClassification, Optional<CaseDetails>> flatmapFunctionV1 = new Function<SecurityClassification,
            Optional<CaseDetails>>() {
            @Override
            public Optional<CaseDetails> apply(SecurityClassification securityClassification) {
                Optional<CaseDetails> caseDetails1 = Optional.of(caseDetails);
                // TODO: Log caseDetails1
                Optional<CaseDetails> caseDetails2 = caseDetails1.filter(
                    caseHasClassificationEqualOrLowerThan(securityClassification));
                // TODO: Log caseDetails2
                Optional<CaseDetails> caseDetails3 = caseDetails2.map(
                    new MapFunctionWrapper(caseDetails, securityClassification).mapFunction);
                // TODO: Log caseDetails3
                return caseDetails3;
            }
        };

        Optional<CaseDetails> caseDetails4 = userClassificationOpt.flatMap(flatmapFunctionV1);
        // TODO: Log caseDetails4
        return caseDetails4;
    }


    /*
     * MODIFIED "version 2" applyClassification(CaseDetails caseDetails, boolean create).
     * ********
     */
    public Optional<CaseDetails> applyClassificationModifiedVersion2(CaseDetails caseDetails, boolean create) {
        jclog("applyClassification (MODIFIED version 2)");
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, create);
        // TODO: Log userClassificationOpt

        Function<SecurityClassification, Optional<CaseDetails>> flatmapFunctionV2 = new Function<SecurityClassification,
            Optional<CaseDetails>>() {
            @Override
            public Optional<CaseDetails> apply(SecurityClassification securityClassification) {
                Optional<CaseDetails> caseDetails1 = Optional.of(caseDetails)
                    .filter(caseHasClassificationEqualOrLowerThan(securityClassification))
                    .map(new MapFunctionWrapper(caseDetails, securityClassification).mapFunction);
                // TODO: Log caseDetails1
                return caseDetails1;
            }
        };

        Optional<CaseDetails> caseDetails4 = userClassificationOpt.flatMap(flatmapFunctionV2);
        // TODO: Log caseDetails4
        return caseDetails4;
    }


    // START OF INNER CLASS mapFunctionWrapper
    class MapFunctionWrapper {
        final CaseDetails caseDetails;
        final SecurityClassification securityClassification;

        MapFunctionWrapper(final CaseDetails caseDetails, final SecurityClassification securityClassification) {
            this.caseDetails = caseDetails;
            this.securityClassification = securityClassification;
        }

        Function<CaseDetails, CaseDetails> mapFunction = new Function<CaseDetails, CaseDetails>() {
            @Override
            public CaseDetails apply(CaseDetails cd) {
                if (cd.getDataClassification() == null) {
                    LOG.warn("No data classification for case with reference={}, all fields removed",
                        cd.getReference());
                    jclog("No data classification for case with reference="
                        + cd.getReference() + ", all fields removed");
                    cd.setDataClassification(Maps.newHashMap());
                }

                JsonNode data = filterNestedObject(JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                    JacksonUtils.convertValueJsonNode(cd.getDataClassification()),
                    securityClassification);
                cd.setData(JacksonUtils.convertValue(data));
                // TODO: Log variable cd  (and JsonNode data ?)
                return cd;
            }
        };
    }
    // END OF INNER CLASS mapFunctionWrapper




    /*
     * Using this method as "test harness" to call both ORIGINAL applyClassification() (-BELOW-)
     * and MODIFIED applyClassification() (-ABOVE-).
     * Called from ClassifiedStartEventOperation (line 102).
     */
    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails) {
        // Call ORIGINAL applyClassification() (method -BELOW-).
        Optional<CaseDetails> original = applyClassification(caseDetails, false);

        // Call MODIFIED applyClassification() (methods -ABOVE-).
        Optional<CaseDetails> modifiedV1 = applyClassificationModifiedVersion1(caseDetails, false);
        Optional<CaseDetails> modifiedV2 = applyClassificationModifiedVersion2(caseDetails, false);

        try {
            final int originalHashCode = objectMapper.writeValueAsString(original).hashCode();
            final int modifiedV1HashCode = objectMapper.writeValueAsString(modifiedV1).hashCode();
            final int modifiedV2HashCode = objectMapper.writeValueAsString(modifiedV2).hashCode();
            jclog("originalHashCode and modifiedV1HashCode: "
                + (originalHashCode == modifiedV1HashCode ? "SAME" : "DIFFER"));
            jclog("originalHashCode and modifiedV2HashCode: "
                + (originalHashCode == modifiedV2HashCode ? "SAME" : "DIFFER"));
        } catch (JsonProcessingException e) {
            jclog("originalHashCode and modifiedHashCode: ERROR: " + e.getMessage());
        }

        return original;
    }




    /*
     * BACKUP OF ORIGINAL applyClassification(CaseDetails caseDetails, boolean create).
     * ******************
     */
    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails, boolean create) {
        jclog("applyClassification (ORIGINAL)");
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, create);
        return userClassificationOpt
            .flatMap(securityClassification ->
                Optional.of(caseDetails).filter(caseHasClassificationEqualOrLowerThan(securityClassification))
                    .map(cd -> {
                        if (cd.getDataClassification() == null) {
                            LOG.warn("No data classification for case with reference={},"
                                + " all fields removed", cd.getReference());
                            cd.setDataClassification(Maps.newHashMap());
                        }

                        JsonNode data = filterNestedObject(JacksonUtils.convertValueJsonNode(caseDetails.getData()),
                            JacksonUtils.convertValueJsonNode(caseDetails.getDataClassification()),
                            securityClassification);
                        caseDetails.setData(JacksonUtils.convertValue(data));
                        return cd;
                    }));
    }

    public List<AuditEvent> applyClassification(CaseDetails caseDetails, List<AuditEvent> events) {
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

    public SecurityClassification getClassificationForEvent(CaseTypeDefinition caseTypeDefinition,
                                                            CaseEventDefinition caseEventDefinition) {
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
        final Optional<SecurityClassification> userClassification =
            getUserClassification(caseTypeDefinition, false);
        return userClassification.map(securityClassification ->
            securityClassification.higherOrEqualTo(caseTypeDefinition.getClassificationForField(fieldId)))
            .orElse(false);
    }

    public boolean userHasEnoughSecurityClassificationForField(CaseTypeDefinition caseTypeDefinition,
                                                               SecurityClassification otherClassification) {
        final Optional<SecurityClassification> userClassification = getUserClassification(caseTypeDefinition, false);
        return userClassification.map(securityClassification ->
            securityClassification.higherOrEqualTo(otherClassification))
            .orElse(false);
    }

    public Optional<SecurityClassification> getUserClassification(CaseTypeDefinition caseTypeDefinition,
                                                                  boolean isCreateProfile) {
        return maxSecurityClassification(caseDataAccessControl
            .getUserClassifications(caseTypeDefinition, isCreateProfile));
    }

    @Override
    public Optional<SecurityClassification> getUserClassification(CaseDetails caseDetails, boolean create) {
        if (create) {
            return maxSecurityClassification(caseDataAccessControl.getUserClassifications(
                caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId()), true));
        }
        return maxSecurityClassification(caseDataAccessControl.getUserClassifications(caseDetails));
    }

    private Optional<SecurityClassification> maxSecurityClassification(Set<SecurityClassification> classifications) {
        return classifications.stream()
            .filter(classification -> classification != null)
            .max(comparingInt(SecurityClassification::getRank));
    }

    private JsonNode filterNestedObject(JsonNode data, JsonNode dataClassification,
                                        SecurityClassification userClassification) {
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
        Optional<SecurityClassification> securityClassification = getSecurityClassification(dataClassificationValue);
        if (!securityClassification.isPresent() || !userClassification.higherOrEqualTo(securityClassification.get())) {
            iterator.remove();
        }
    }

    private boolean isAnyNull(JsonNode... jsonNodes) {
        return newArrayList(jsonNodes).stream().anyMatch(Objects::isNull);
    }
}
