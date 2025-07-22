package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.CaseTypeResults;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER)
@Slf4j
public class ElasticsearchCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "ElasticsearchCaseSearchOperation";
    static final String MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE = "root_cause";
    private static final String HITS = "hits";

    private final ElasticsearchClient elasticsearchClient;
    private final JsonpMapper jsonpMapper;
    private final ObjectMapper objectMapper;
    private final CaseDetailsMapper caseDetailsMapper;
    private final ApplicationParams applicationParams;
    private final CaseSearchRequestSecurity caseSearchRequestSecurity;

    @Autowired
    public ElasticsearchCaseSearchOperation(ElasticsearchClient elasticsearchClient,
                                            @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                            CaseDetailsMapper caseDetailsMapper,
                                            ApplicationParams applicationParams,
                                            CaseSearchRequestSecurity caseSearchRequestSecurity,
                                            JsonpMapper jsonpMapper) {
        this.elasticsearchClient = elasticsearchClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.applicationParams = applicationParams;
        this.caseSearchRequestSecurity = caseSearchRequestSecurity;
        this.jsonpMapper = jsonpMapper;
    }

    @Override
    public CaseSearchResult execute(CrossCaseTypeSearchRequest request, boolean dataClassification) {
        MsearchResponse<ElasticSearchCaseDetailsDTO> result = search(request);
        return toCaseDetailsSearchResult(result, request);
    }

    private MsearchResponse<ElasticSearchCaseDetailsDTO> search(CrossCaseTypeSearchRequest request) {
        try {
            List<RequestItem> searches = request.getSearchIndex()
                .map(index -> List.of(createSearchItem(index.getIndexName(), request)))
                .orElseGet(() -> buildSearchItemsByCaseType(request));

            MsearchRequest msearchRequest = new MsearchRequest.Builder()
                .searches(searches)
                .build();

            return elasticsearchClient.msearch(msearchRequest, ElasticSearchCaseDetailsDTO.class);
        } catch (IOException e) {
            throw new ServiceException("Exception executing Elasticsearch search: " + e.getMessage(), e);
        }
    }

    private List<RequestItem> buildSearchItemsByCaseType(CrossCaseTypeSearchRequest request) {
        return request.getCaseTypeIds()
            .stream()
            .map(caseTypeId -> createSearchItem(getCaseIndexName(caseTypeId), request))
            .collect(Collectors.toList());
    }

    private RequestItem createSearchItem(String indexName, CrossCaseTypeSearchRequest request) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(
            new CaseSearchRequest(indexName, request.getElasticSearchRequest())
        );

        log.info("Executing search request:- index:<{}> query:{}", indexName,
            securedSearchRequest.toElasticsearchJsonParser(jsonpMapper));

        try {
            RequestItem requestItem = RequestItem.of(r -> r
                .header(h -> h.index(indexName))
                .body(b -> b.query(q -> q.withJson(
                    securedSearchRequest.toElasticsearchJsonParser(jsonpMapper), jsonpMapper)))
            );
            return requestItem;
        } catch (BadSearchRequest e) {
            // If it was a validation issue, rethrow it
            throw e;
        } catch (Exception e) {
            // For all other unexpected errors (like parser failures), wrap in ServiceException
            throw new ServiceException("Failed to build request item from JSON", e);
        }
    }

    private CaseSearchResult toCaseDetailsSearchResult(
        MsearchResponse<ElasticSearchCaseDetailsDTO> multiSearchResult,
        CrossCaseTypeSearchRequest request
    ) {
        List<CaseDetails> allCaseDetails = new ArrayList<>();
        List<CaseTypeResults> caseTypeResults = new ArrayList<>();
        long total = 0;

        for (MultiSearchResponseItem<ElasticSearchCaseDetailsDTO> response : multiSearchResult.responses()) {
            if (response.isFailure()) {
                String errorMsg = Optional.ofNullable(response.failure())
                    .map(e -> e.error() + " [" + e.status() + "]")
                    .orElse("Unknown search failure");
                log.error("Elasticsearch search error: {}", errorMsg);
                throw new BadSearchRequest(errorMsg);
            }

            var result = response.result();
            List<Hit<ElasticSearchCaseDetailsDTO>> hits = result.hits().hits();
            if (!hits.isEmpty()) {
                String index = hits.get(0).index();
                String caseTypeId = request.getSearchIndex().isEmpty()
                    ? getCaseTypeIDFromIndex(index, request.getCaseTypeIds())
                    : null;

                List<ElasticSearchCaseDetailsDTO> dtos = hits.stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                List<CaseDetails> mapped = caseDetailsMapper.dtosToCaseDetailsList(dtos);
                allCaseDetails.addAll(mapped);

                long count = Optional.ofNullable(result.hits().total())
                    .map(TotalHits::value)
                    .orElse((long) mapped.size());

                caseTypeResults.add(new CaseTypeResults(caseTypeId, count));
                total += count;
            }
        }

        return new CaseSearchResult(total, allCaseDetails, caseTypeResults);
    }

    private String getCaseTypeIDFromIndex(String index, List<String> caseTypeIds) {
        String patternString = applicationParams.getCasesIndexNameCaseTypeIdGroup();
        int group = applicationParams.getCasesIndexNameCaseTypeIdGroupPosition();
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(index);

        if (matcher.matches() && matcher.groupCount() >= group) {
            String extracted = matcher.group(group);
            return caseTypeIds.stream()
                .filter(id -> id.equalsIgnoreCase(extracted))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Index '{}' resolved unknown caseTypeId: '{}'", index, extracted);
                    return new ServiceException("Cannot determine case type id from ES index");
                });
        }

        log.error("Index '{}' did not match pattern '{}' or group missing", index, patternString);
        throw new ServiceException("Cannot extract case type id from ES index name");
    }

    private String getCaseIndexName(String caseTypeId) {
        return format(applicationParams.getCasesIndexNameFormat(), caseTypeId.toLowerCase());
    }
}
