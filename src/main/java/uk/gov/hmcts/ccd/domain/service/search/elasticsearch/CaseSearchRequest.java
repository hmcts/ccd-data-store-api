package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

/**
 * Sample ES json search request.
 * <p>
 * {
 * 	"query": {
 * 		"bool": {
 * 			"filter": {
 * 				"match": { "state": "AwaitingPayment"}
 *                }
 *            }
 *       },
 * 	"sort": {
 * 		"id": { "order":"asc" }
 *       }
 * }
 */
@Slf4j
public class CaseSearchRequest {

    private static final String QUERY_NAME = "query";

    private final ObjectMapperService objectMapperService;
    private final String caseTypeId;
    private final ObjectNode searchRequestJsonNode;


    public CaseSearchRequest(ObjectMapperService objectMapperService, String caseTypeId, String jsonSearchRequest) {
        this.objectMapperService = objectMapperService;
        this.caseTypeId = caseTypeId;
        this.searchRequestJsonNode = this.objectMapperService.convertStringToObject(jsonSearchRequest, ObjectNode.class);
        validateJsonSearchRequest();
    }

    private void validateJsonSearchRequest() {
        if (!searchRequestJsonNode.has(QUERY_NAME)) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    String getQueryValue() {
        return searchRequestJsonNode.get(QUERY_NAME).toString();
    }

    void replaceQuery(String queryClause) {
        ObjectNode queryClauseNode = objectMapperService.convertStringToObject(queryClause, ObjectNode.class);
        searchRequestJsonNode.set(QUERY_NAME, queryClauseNode.get(QUERY_NAME));
    }

    String toJsonString() {
        String jsonString = searchRequestJsonNode.toString();
        log.debug("json search request: {}", jsonString);
        return jsonString;
    }
}
