package uk.gov.hmcts.ccd.domain.service.jsonpath;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.Map;

@Service
public class CaseDetailsJsonParser {

    private static final String COLLECTION_PATH = "[";
    private static final String ID_EXPRESSION_START = "[?(@.id == \"";
    private static final String ID_EXPRESSION_END = "\")].value";
    private static final Configuration configuration = Configuration.defaultConfiguration()
        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
        .addOptions(Option.SUPPRESS_EXCEPTIONS);

    public boolean containsDocumentUrl(CaseDetails caseDetails, String attributePath) {
        Map data = read(caseDetails, attributePath);
        return data.get("document_url") != null;
    }

    public Map read(CaseDetails caseDetails, String attributePath) {
        String existingCaseData = convertToJson(caseDetails);
        String path = compiledPath(attributePath);
        if (isArrayPath(attributePath)) {
            List jsonArray = JsonPath.read(existingCaseData, path);
            return jsonArray.size() > 0 ? (Map) jsonArray.get(0) : Maps.newHashMap();
        }
        return readData(existingCaseData, path);
    }

    private Map readData(String existingCaseData, String path) {
        Object data = JsonPath.read(existingCaseData, path);
        if (data instanceof Map) {
            return (Map) data;
        }
        return Maps.newHashMap();
    }

    public void updateCaseDocumentData(String attributePath, String value, CaseDetails caseDetails) {
        String jsonPath = compiledPath(attributePath) + ".category_id";
        DocumentContext documentContext = JsonPath.using(configuration).parse(convertToJson(caseDetails));
        documentContext.set(JsonPath.compile(jsonPath), value);
        try {
            caseDetails.setData(JacksonUtils.convertJsonNode(documentContext.jsonString()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to process updated Case Details Data");
        }
    }

    public String compiledPath(String attributePath) {
        return compiledPath(attributePath, true);
    }

    public String compiledPath(String attributePath, boolean addJsonPathNotation) {
        if (isArrayPath(attributePath)) {
            attributePath = attributePath.replaceAll("\\[", ID_EXPRESSION_START);
            attributePath = attributePath.replaceAll("]", ID_EXPRESSION_END);
        }
        return addJsonPathNotation ? "$." + attributePath : attributePath;
    }

    private String convertToJson(CaseDetails caseDetails) {
        try {
            return JacksonUtils.writeValueAsString(caseDetails.getData());
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to process Case Details Data");
        }
    }

    private boolean isArrayPath(String attributePath) {
        return attributePath.contains(COLLECTION_PATH);
    }
}
