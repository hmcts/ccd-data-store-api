package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

// TODO CaseService and CaseDataService could probably be merged together.
@Service
@SuppressWarnings("checkstyle:SummaryJavadoc")
// partal javadoc attributes added prior to checkstyle implementation in module
public class CaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CaseService.class);

    private final CaseDataService caseDataService;
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDService uidService;

    private void jclog(String message) {
        LOG.info("JCDEBUG: Info: CaseService: {}", message);
        LOG.warn("JCDEBUG: Warn: CaseService: {}", message);
        LOG.error("JCDEBUG: Error: CaseService: {}", message);
        LOG.debug("JCDEBUG: Debug: CaseService: {}", message);
    }

    @Autowired
    public CaseService(CaseDataService caseDataService,
                       @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                            final CaseDetailsRepository caseDetailsRepository,
                       UIDService uidService) {
        this.caseDataService = caseDataService;
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
    }

    /**
     * Generate a SHA256 hash of the case data serialised as JSON.
     *
     * @param caseDetails Case whose data will be hashed
     * @return SHA256 hash of the given case data
     */
    public String hashData(CaseDetails caseDetails) {
        final JsonNode jsonData = JacksonUtils.convertValueJsonNode(caseDetails.getData());
        return DigestUtils.sha256Hex(jsonData.toString());
    }

    /**
     * @param caseTypeId     caseTypeId of new case details
     * @param jurisdictionId jurisdictionId of new case details
     * @return <code>CaseDetails</code> - new case details object
     */
    public CaseDetails createNewCaseDetails(String caseTypeId, String jurisdictionId, Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setJurisdiction(jurisdictionId);
        caseDetails.setData(data == null ? Maps.newHashMap() : data);
        return caseDetails;
    }

    /**
     * @param content     Data received from the client.
     * @param caseDetails of the case.
     * @return <code>Optional&lt;CaseDetails&gt;</code> - CaseDetails wrapped in Optional
     */
    public CaseDetails populateCurrentCaseDetailsWithEventFields(CaseDataContent content, CaseDetails caseDetails) {
        final Map<String, JsonNode> eventData = content.getEventData();
        Map<String, JsonNode> caseData = caseDetails.getData();

        // LOG eventData
        eventData.forEach((key, value) -> jclog("eventData: " + key + " = " + value));

        // LOG caseDataBefore
        caseData.forEach((key, value) -> jclog("caseDataBefore: " + key + " = " + value));

        // Process eventData -> caseData
        eventData.forEach((key, value) -> caseData.put(key, value));
        caseDetails.setData(caseData);

        // LOG caseDataAfter
        caseData.forEach((key, value) -> jclog("caseDataAfter: " + key + " = " + value));

        return caseDetails;
    }

    public CaseDetails clone(CaseDetails source) {
        final CaseDetails clone;

        try {
            clone = source.shallowClone();
        } catch (CloneNotSupportedException e) {
            // Trivial exception as DetailsCase implements Cloneable interface
            throw new IllegalArgumentException("Case details cannot be cloned", e);
        }

        // Deep cloning of mutable properties
        clone.setData(caseDataService.cloneDataMap(source.getData()));
        clone.setDataClassification(caseDataService.cloneDataMap(source.getDataClassification()));

        return clone;
    }

    public CaseDetails getCaseDetails(String jurisdictionId, String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }
        final Optional<CaseDetails> caseDetails =
            caseDetailsRepository.findByReference(jurisdictionId, Long.valueOf(caseReference));
        return caseDetails.orElseThrow(() -> new ResourceNotFoundException("No case exist with id=" + caseReference));
    }

    public CaseDetails getCaseDetailsByCaseReference(String caseReference) {
        final Optional<CaseDetails> caseDetails =
            caseDetailsRepository.findByReferenceWithNoAccessControl(caseReference);
        return caseDetails.orElseThrow(() -> new ResourceNotFoundException("No case exist with id=" + caseReference));
    }

    /**
     * Builds a json representation of the caseFields with a defaultValue present.
     * Has no knowledge of the collections, hence all ArrayNodes are represented as an ObjectNode.
     */
    public Map<String, JsonNode> buildJsonFromCaseFieldsWithDefaultValue(
        List<CaseEventFieldDefinition> caseEventDefinition) {
        Map<String, JsonNode> data = new HashMap<>();

        caseEventDefinition.forEach(
            caseField -> {

                String defaultValue = caseField.getDefaultValue();
                if (defaultValue != null) {
                    data.put(caseField.getCaseFieldId(), MAPPER.getNodeFactory().textNode(defaultValue));
                }

                List<JsonNode> collect = caseField.getCaseEventFieldComplexDefinitions().stream()
                    .filter(e -> e.getDefaultValue() != null)
                    .filter(e -> !e.getReference().isBlank())
                    .map(caseEventFieldComplex -> JacksonUtils
                        .buildFromDottedPath(caseEventFieldComplex.getReference(),
                                             caseEventFieldComplex.getDefaultValue())).collect(toList());

                if (!collect.isEmpty()) { // to prevent construct like "FieldA": {}
                    ObjectNode objectNode = MAPPER.getNodeFactory().objectNode();
                    collect.forEach(e -> {
                        String next = e.fieldNames().next();
                        objectNode.set(next, e.findValue(next));
                    });
                    data.put(caseField.getCaseFieldId(), objectNode);
                }
            });

        return data;
    }


    public Map<String, JsonNode> buildJsonFromCaseFieldsWithNullifyByDefault(CaseTypeDefinition caseTypeDefinition,
        List<CaseEventFieldDefinition> caseEventDefinition) {
        Map<String, JsonNode> data = new HashMap<>();

        caseEventDefinition.forEach(
            caseField -> {
                Boolean nullifyByDefault = caseField.getNullifyByDefault();
                if (Boolean.TRUE.equals(nullifyByDefault)) {
                    Optional<CaseFieldDefinition> caseFieldDefinition = caseTypeDefinition
                        .getCaseField(caseField.getCaseFieldId());
                    if (caseFieldDefinition.isPresent()) {
                        if (caseFieldDefinition.get().isCollectionFieldType()) {
                            data.put(caseField.getCaseFieldId(), MAPPER.getNodeFactory().arrayNode());
                        } else if (caseFieldDefinition.get().isComplexFieldType()) {
                            data.put(caseField.getCaseFieldId(), MAPPER.getNodeFactory().objectNode());
                        } else {
                            data.put(caseField.getCaseFieldId(), MAPPER.getNodeFactory().nullNode());
                        }
                    }
                }
            });
        return data;
    }
}
