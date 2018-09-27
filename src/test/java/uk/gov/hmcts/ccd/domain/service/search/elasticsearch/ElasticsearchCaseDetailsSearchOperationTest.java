package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchQuerySecurity;
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchRequestFactory;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

<<<<<<< HEAD:src/test/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticSearchCaseDetailsSearchOperationTest.java
import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchCaseDetailsSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final List<String> CASE_TYPES_ID = newArrayList("caseTypeId1", "caseTypeId2");
=======
class ElasticsearchCaseDetailsSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final String CASE_TYPE_ID = "casetypeid";
>>>>>>> c3eb9b8... RDM-2811 - add security to search endpoint (Elasticsearch):src/test/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticsearchCaseDetailsSearchOperationTest.java
    private static final String INDEX_TYPE = "case";
    private String caseDetailsElastic = "{some case details}";

    @InjectMocks
    private ElasticsearchCaseDetailsSearchOperation searchOperation;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private JestClient jestClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CaseDetailsMapper mapper;

    @Mock
    private ElasticSearchCaseDetailsDTO caseDetailsDTO;

    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        CaseSearchRequestFactory<Search> caseSearchRequestFactory = new ElasticsearchCaseSearchRequestFactory(applicationParams,
                                                                                                              mock(CaseSearchQuerySecurity.class));
        Whitebox.setInternalState(searchOperation, "caseSearchRequestFactory", caseSearchRequestFactory);
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
    }

    @Test
    void searchShouldMapElasticSearchResultToSearchResult() throws IOException {
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.isSucceeded()).thenReturn(true);
        when(searchResult.getTotal()).thenReturn(1L);
        when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
        when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
        when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
        when(jestClient.execute(any(Search.class))).thenReturn(searchResult);

        CaseDetailsSearchResult caseDetailsSearchResult = searchOperation.execute(CASE_TYPES_ID, "{query}");

        assertThat(caseDetailsSearchResult.getCases(), equalTo(newArrayList(caseDetails)));
        assertThat(caseDetailsSearchResult.getTotal(), equalTo(1L));
        ArgumentCaptor<Search> arg = ArgumentCaptor.forClass(Search.class);
        verify(jestClient).execute(arg.capture());
        Search searchRequest = arg.getValue();
        assertThat(searchRequest.getIndex(), equalTo(indices(CASE_TYPES_ID)));
        assertThat(searchRequest.getType(), equalTo(INDEX_TYPE));
    }

    @Test
    void searchShouldReturnBadSearchRequestOnFailure() throws IOException {
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.isSucceeded()).thenReturn(false);
        when(jestClient.execute(any(Search.class))).thenReturn(searchResult);

        assertThrows(BadSearchRequest.class, () -> searchOperation.execute(CASE_TYPES_ID, "{query}"));
    }

    private String indices(List<String> caseTypesId) {
        return join(caseTypesId.stream().map(caseTypeId ->
                String.format(INDEX_NAME_FORMAT, caseTypeId))
                .collect(toList()), ",");
    }
}