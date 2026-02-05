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

    protected static final String FROM = "from";
    protected static final String GT = "gt";
    protected static final String GTE = "gte";
    protected static final String LT = "lt";
    protected static final String LTE = "lte";
    protected static final String INCLUDE_LOWER = "include_lower";
    protected static final String INCLUDE_UPPER = "include_upper";
    protected static final String RANGE = "range";
    protected static final String TO = "to";

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
            JsonNode fieldNode = rangeNode.get(fieldName);
            if (fieldNode == null || !fieldNode.isObject()) {
                return;
            }

            ObjectNode fieldObject = (ObjectNode) fieldNode;
            boolean hasFrom = fieldObject.has(FROM);
            boolean hasTo = fieldObject.has(TO);
            if (!hasFrom && !hasTo) {
                return;
            }

            boolean includeLower = !fieldObject.has(INCLUDE_LOWER)
                || fieldObject.get(INCLUDE_LOWER).asBoolean(true);
            boolean includeUpper = !fieldObject.has(INCLUDE_UPPER)
                || fieldObject.get(INCLUDE_UPPER).asBoolean(true);

            if (hasFrom && !fieldObject.has(GTE) && !fieldObject.has(GT)) {
                JsonNode fromValue = fieldObject.get(FROM);
                if (fromValue != null && !fromValue.isNull()) {
                    fieldObject.set(includeLower ? GTE : GT, fromValue);
                }
            }
            if (hasTo && !fieldObject.has(LTE) && !fieldObject.has(LT)) {
                JsonNode toValue = fieldObject.get(TO);
                if (toValue != null && !toValue.isNull()) {
                    fieldObject.set(includeUpper ? LTE : LT, toValue);
                }
            }

            if (hasFrom) {
                fieldObject.remove(FROM);
            }
            if (hasTo) {
                fieldObject.remove(TO);
            }
            fieldObject.remove(INCLUDE_LOWER);
            fieldObject.remove(INCLUDE_UPPER);
        });
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
