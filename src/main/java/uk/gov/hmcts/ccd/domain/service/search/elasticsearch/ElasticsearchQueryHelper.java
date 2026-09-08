package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SUPPLEMENTARY_DATA;

@Service
@Slf4j
public class ElasticsearchQueryHelper {

    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UserRepository userRepository;

    private static final String FROM = "from";
    private static final String GT = "gt";
    private static final String GTE = "gte";
    private static final String LT = "lt";
    private static final String LTE = "lte";
    private static final String INCLUDE_LOWER = "include_lower";
    private static final String INCLUDE_UPPER = "include_upper";
    private static final String RANGE = "range";
    private static final String TO = "to";

    @Autowired
    public ElasticsearchQueryHelper(ApplicationParams applicationParams,
                                    ObjectMapperService objectMapperService,
                                    @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository,
                                    @Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository) {
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.userRepository = userRepository;
    }

    public List<String> getCaseTypesAvailableToUser() {
        if (userRepository.anyRoleEqualsAnyOf(applicationParams.getCcdAccessControlCrossJurisdictionRoles())) {
            return caseDefinitionRepository.getAllCaseTypesIDs();
        } else {
            return getCaseTypesFromIdamRoles();
        }
    }

    private List<String> getCaseTypesFromIdamRoles() {
        List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();
        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
    }

    public ElasticsearchRequest validateAndConvertRequest(String jsonSearchRequest) {
        rejectBlackListedQuery(jsonSearchRequest);
        JsonNode searchRequestNode;
        try {
            searchRequestNode = objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class);
        } catch (ServiceException ex) {
            throw new BadRequestException("Request requires correctly formatted JSON, " + ex.getMessage());
        }
        normaliseRangeQueries(searchRequestNode);
        validateSupplementaryData(searchRequestNode);
        return new ElasticsearchRequest(searchRequestNode);
    }

    private void normaliseRangeQueries(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            JsonNode rangeNode = objectNode.get(RANGE);
            if (rangeNode != null && rangeNode.isObject()) {
                normaliseRangeObject((ObjectNode) rangeNode);
            }

            Iterator<JsonNode> elements = objectNode.elements();
            while (elements.hasNext()) {
                normaliseRangeQueries(elements.next());
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode element : node) {
                normaliseRangeQueries(element);
            }
        }
    }

    private void normaliseRangeObject(ObjectNode rangeNode) {
        rangeNode.fieldNames().forEachRemaining(fieldName -> {
            ObjectNode fieldObject = asObject(rangeNode.get(fieldName));
            if (fieldObject == null) {
                return;
            }
            applyRangeFieldUpdates(fieldObject);
        });
    }

    private ObjectNode asObject(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return (ObjectNode) node;
    }

    private void applyRangeFieldUpdates(ObjectNode fieldObject) {
        boolean hasFrom = fieldObject.has(FROM);
        boolean hasTo = fieldObject.has(TO);
        if (!hasFrom && !hasTo) {
            return;
        }

        boolean includeLower = !fieldObject.has(INCLUDE_LOWER)
            || fieldObject.get(INCLUDE_LOWER).asBoolean(true);
        boolean includeUpper = !fieldObject.has(INCLUDE_UPPER)
            || fieldObject.get(INCLUDE_UPPER).asBoolean(true);

        if (hasFrom) {
            setRangeValue(fieldObject, FROM, includeLower ? GTE : GT, GTE, GT);
        }
        if (hasTo) {
            setRangeValue(fieldObject, TO, includeUpper ? LTE : LT, LTE, LT);
        }

        removeRangeMarkers(fieldObject, hasFrom, hasTo);
    }

    private void setRangeValue(ObjectNode fieldObject,
                               String legacyKey,
                               String targetKey,
                               String inclusiveKey,
                               String exclusiveKey) {
        if (fieldObject.has(inclusiveKey) || fieldObject.has(exclusiveKey)) {
            return;
        }
        JsonNode value = fieldObject.get(legacyKey);
        if (value != null && !value.isNull()) {
            fieldObject.set(targetKey, value);
        }
    }

    private void removeRangeMarkers(ObjectNode fieldObject, boolean hasFrom, boolean hasTo) {
        if (hasFrom) {
            fieldObject.remove(FROM);
        }
        if (hasTo) {
            fieldObject.remove(TO);
        }
        fieldObject.remove(INCLUDE_LOWER);
        fieldObject.remove(INCLUDE_UPPER);
    }

    private void rejectBlackListedQuery(String jsonSearchRequest) {
        List<String> blackListedQueries = applicationParams.getSearchBlackList();
        Optional<String> blackListedQueryOpt = blackListedQueries
            .stream()
            .filter(blacklisted -> {
                Pattern p = Pattern.compile("\\b" + blacklisted + "\\b");
                Matcher m = p.matcher(jsonSearchRequest);
                return m.find();
            })
            .findFirst();
        blackListedQueryOpt.ifPresent(blacklisted -> {
            throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
        });
    }

    private void validateSupplementaryData(JsonNode searchRequest) {
        JsonNode supplementaryDataNode = searchRequest.get(SUPPLEMENTARY_DATA);
        if (supplementaryDataNode != null && !isArrayOfTextFields(supplementaryDataNode)) {
            throw new BadSearchRequest("Requested supplementary_data must be an array of text fields.");
        }
    }

    private boolean isArrayOfTextFields(JsonNode node) {
        return node.isArray() && arrayContainsOnlyText((ArrayNode) node);
    }

    private boolean arrayContainsOnlyText(ArrayNode node) {
        return StreamSupport.stream(node.spliterator(), false)
            .allMatch(JsonNode::isTextual);
    }
}
