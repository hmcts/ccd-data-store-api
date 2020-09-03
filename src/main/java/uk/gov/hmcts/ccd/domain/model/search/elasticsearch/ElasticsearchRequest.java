package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import lombok.NonNull;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.DATA_CLASSIFICATION_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.DATA_COL;

@Data
public class ElasticsearchRequest {

    public static final String SORT = "sort";
    public static final String SOURCE = "_source";
    public static final String QUERY = "query";
    public static final String NATIVE_ES_QUERY = "native_es_query";
    public static final String SUPPLEMENTARY_DATA = "supplementary_data";
    public static final String CASE_DATA_PREFIX = "data.";
    public static final String SUPPLEMENTARY_DATA_PREFIX = "supplementary_data.";
    public static final String COLLECTION_VALUE_SUFFIX = ".value";
    public static final String WILDCARD = "*";
    public static final ArrayNode METADATA_FIELDS;

    private JsonNode nativeSearchRequest;
    private ArrayNode supplementaryData;

    static {
        METADATA_FIELDS = MAPPER.createArrayNode();
        Arrays.stream(MetaData.CaseField.values())
            .map(field -> new TextNode(field.getDbColumnName()))
            .forEach(METADATA_FIELDS::add);
        METADATA_FIELDS.add(new TextNode(DATA_CLASSIFICATION_COL));
    }

    public ElasticsearchRequest(@NonNull JsonNode searchRequest) {
        initRequest(searchRequest);
    }

    private void initRequest(JsonNode searchRequest) {
        if (searchRequest.has(NATIVE_ES_QUERY)) {
            setNativeSearchRequest(searchRequest.get(NATIVE_ES_QUERY));
            if (searchRequest.has(SUPPLEMENTARY_DATA)) {
                setSupplementaryData(searchRequest.get(SUPPLEMENTARY_DATA));
            }
        } else {
            setNativeSearchRequest(searchRequest);
        }
    }

    public boolean isSorted() {
        return nativeSearchRequest.has(SORT);
    }

    public JsonNode getSort() {
        return nativeSearchRequest.get(SORT);
    }

    public void setSort(ArrayNode sortNode) {
        ((ObjectNode) nativeSearchRequest).set(SORT, sortNode);
    }

    public boolean hasSourceFields() {
        // If a source is empty, boolean or only has a wildcard element, then equivalent to no provided source
        if (this.getSource() instanceof BooleanNode) {
            return false;
        }

        return nativeSearchRequest.has(SOURCE) && !getSource().isEmpty()
            && !(getSource().size() == 1 && getSource().get(0).asText().equals(WILDCARD));
    }

    public JsonNode getSource() {
        return nativeSearchRequest.get(SOURCE);
    }

    public List<String> getRequestedFields() {
        if (hasSourceFields() && getSource().isArray()) {
            return StreamSupport.stream(getSource().spliterator(), false)
                .map(JsonNode::asText)
                .map(this::getFieldId)
                .filter(Objects::nonNull)
                .collect(toList());
        }

        return Collections.emptyList();
    }

    public boolean hasQuery() {
        return nativeSearchRequest.has(QUERY);
    }

    public JsonNode getQuery() {
        return nativeSearchRequest.get(QUERY);
    }

    public boolean hasSupplementaryData() {
        return supplementaryData != null && supplementaryData.isArray();
    }

    public JsonNode getSupplementaryData() {
        return supplementaryData;
    }

    public void setSupplementaryData(JsonNode node) {
        if (node.isArray() && arrayContainsOnlyText((ArrayNode) node)) {
            this.supplementaryData = (ArrayNode) node;
        } else {
            throw new BadSearchRequest("Provided supplementary_data must be an array of text fields.");
        }
    }

    /**
     * Creates a JSON string representing the Elasticsearch request object.
     * Custom properties supported by CCD will be merged appropriately to generate a native Elasticsearch request.
     * @return JSON string representing the Elasticsearch request object.
     */
    public String toJson() {
        JsonNode mergedRequest = nativeSearchRequest.deepCopy();

        ((ObjectNode) mergedRequest).set(SOURCE, mergedSourceFields());

        return mergedRequest.toString();
    }

    private ArrayNode mergedSourceFields() {
        ArrayNode sourceFields = METADATA_FIELDS.deepCopy();

        if (hasSourceFields()) {
            sourceFields.addAll((ArrayNode) getSource());
        } else {
            sourceFields.add(new TextNode(DATA_COL));
        }

        if (hasSupplementaryData()) {
            StreamSupport.stream(getSupplementaryData().spliterator(), false)
                .forEach(field -> sourceFields.add(new TextNode(SUPPLEMENTARY_DATA_PREFIX + field.asText())));
        }

        return sourceFields;
    }

    private String getFieldId(String fieldSourceName) {
        if (fieldSourceName.startsWith(CASE_DATA_PREFIX)) {
            return fieldSourceName.replace(CASE_DATA_PREFIX, "");
        } else {
            try {
                return MetaData.CaseField.valueOfColumnName(fieldSourceName).getReference();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private boolean arrayContainsOnlyText(ArrayNode node) {
        return StreamSupport.stream(node.spliterator(), false)
            .allMatch(JsonNode::isTextual);
    }
}
