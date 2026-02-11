package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.CaseTypeResults;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

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

    private final ObjectMapper objectMapper;
    private final ElasticsearchClient elasticsearchClient;
    private final CaseDetailsMapper caseDetailsMapper;
    private final ApplicationParams applicationParams;
    private final CaseSearchRequestSecurity caseSearchRequestSecurity;

    @Autowired
    public ElasticsearchCaseSearchOperation(ElasticsearchClient elasticsearchClient,
                                            @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                            CaseDetailsMapper caseDetailsMapper,
                                            ApplicationParams applicationParams,
                                            CaseSearchRequestSecurity caseSearchRequestSecurity) {
        this.elasticsearchClient = elasticsearchClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.applicationParams = applicationParams;
        this.caseSearchRequestSecurity = caseSearchRequestSecurity;
    }

    @Override
    public CaseSearchResult execute(CrossCaseTypeSearchRequest request, boolean dataClassification) {
        MsearchResponse<ElasticSearchCaseDetailsDTO> result = search(request);
        return toCaseDetailsSearchResult(result, request);
    }

    private MsearchResponse<ElasticSearchCaseDetailsDTO> search(CrossCaseTypeSearchRequest request) {
        MsearchRequest msearchRequest = secureAndTransformSearchRequest(request);
        log.debug("MsearchRequest: {}", msearchRequest);
        try {
            MsearchResponse<ElasticSearchCaseDetailsDTO> response = elasticsearchClient.msearch(msearchRequest,
                ElasticSearchCaseDetailsDTO.class);
            log.debug("MsearchResponse: {}", response);
            return response;
        } catch (Exception e) {
            throw new ServiceException("Exception executing Elasticsearch search: " + e.getMessage(), e);
        }
    }

    private MsearchRequest secureAndTransformSearchRequest(CrossCaseTypeSearchRequest request) {
        final List<RequestItem> securedSearches = request.getSearchIndex()
            .map(searchIndex -> List.of(createSecuredRequestItem(searchIndex, request)))
            .orElseGet(() -> buildRequestItemsByCaseType(request));
        return new MsearchRequest.Builder().searches(securedSearches).build();
    }

    private List<RequestItem> buildRequestItemsByCaseType(final CrossCaseTypeSearchRequest request) {
        return request.getCaseTypeIds()
            .stream()
            .map(caseTypeId -> createSecuredRequestItem(caseTypeId, request))
            .toList();
    }

    private RequestItem createSecuredRequestItem(final SearchIndex searchIndex,
                                                 final CrossCaseTypeSearchRequest request) {
        final CrossCaseTypeSearchRequest securedSearchRequest =
            caseSearchRequestSecurity.createSecuredSearchRequest(request);

        final ElasticsearchRequest elasticSearchRequest = securedSearchRequest.getElasticSearchRequest();
        return ElasticsearchMsearchRequestBuilder.createRequestItem(
            searchIndex.getIndexName(),
            elasticSearchRequest.toFinalRequest()
        );
    }

    private RequestItem createSecuredRequestItem(String caseTypeId, CrossCaseTypeSearchRequest request) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(
            new CaseSearchRequest(caseTypeId, request.getElasticSearchRequest()));
        return ElasticsearchMsearchRequestBuilder.createRequestItem(
            getCaseIndexName(caseTypeId),
            securedSearchRequest.toJsonString()
        );
    }

    private CaseSearchResult toCaseDetailsSearchResult(MsearchResponse<ElasticSearchCaseDetailsDTO> multiSearchResult,
                                                       CrossCaseTypeSearchRequest request) {
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
            if (result == null || result.hits() == null || result.hits().hits() == null) {
                log.warn("No hits found for index: {}", "");
                continue;
            }
            assert result.hits().total() != null;
            total += result.hits().total().value();
            List<Hit<ElasticSearchCaseDetailsDTO>> hits = result.hits().hits();
            if (!hits.isEmpty()) {
                String indexName = hits.getFirst().index();
                String caseTypeId = request.getSearchIndex().isEmpty()
                    ? getCaseTypeIDFromIndex(indexName, request.getCaseTypeIds())
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
            }
        }

        return new CaseSearchResult(total, allCaseDetails, caseTypeResults);
    }

    private String getCaseTypeIDFromIndex(final String index, List<String> caseTypeIds) {
        String caseTypeIdGroupRegex = applicationParams.getCasesIndexNameCaseTypeIdGroup();
        int caseTypeIdGroupPosition = applicationParams.getCasesIndexNameCaseTypeIdGroupPosition();
        Pattern pattern = Pattern.compile(caseTypeIdGroupRegex);
        Matcher m = pattern.matcher(index);
        if (m.matches() && m.groupCount() > 1) {
            return caseTypeIds.stream().filter(
                caseTypeId -> caseTypeId.equalsIgnoreCase(m.group(caseTypeIdGroupPosition))
            ).findFirst().orElseThrow(() -> {
                log.error("Cannot match any known case type id from index '{}' extracted case type id : {}",
                           index, m.group(caseTypeIdGroupPosition));
                throw new ServiceException("Cannot determine case type id from ES index name - unknown "
                    + "extracted case type id");
            });
        } else {
            log.error("Cannot determine case type id from index name: '{}'. No capturing group configured or capturing"
                + " group not matching: '{}'.",
                index, caseTypeIdGroupRegex);
            throw new ServiceException("Cannot determine case type id from ES index name - cannot extract"
                + " case type id");
        }
    }

    private String getCaseIndexName(String caseTypeId) {
        return format(applicationParams.getCasesIndexNameFormat(), caseTypeId.toLowerCase());
    }

}
