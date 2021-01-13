package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

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

    @Autowired
    public ElasticsearchQueryHelper(ApplicationParams applicationParams,
                                    ObjectMapperService objectMapperService) {
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
    }

    public ElasticsearchRequest validateAndConvertRequest(String jsonSearchRequest) {
        rejectBlackListedQuery(jsonSearchRequest);
        JsonNode searchRequestNode = objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class);
        validateSupplementaryData(searchRequestNode);
        return new ElasticsearchRequest(searchRequestNode);
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
