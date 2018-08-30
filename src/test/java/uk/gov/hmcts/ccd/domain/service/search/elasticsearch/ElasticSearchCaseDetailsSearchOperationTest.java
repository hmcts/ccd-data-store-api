package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchCaseDetailsSearchOperationTest {

    private static final String INDEX_NAME_FORMAT = "%s_cases";
    private static final List<String> CASE_TYPES_ID = newArrayList("caseTypeId1", "caseTypeId2");
    private static final String INDEX_TYPE = "case";
    private String caseDetailsElastic = "{some case details}";

    @InjectMocks
    private ElasticSearchCaseDetailsSearchOperation searchOperation;

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

    @Before
    public void setup() {
        when(applicationParams.getCasesIndexNameFormat()).thenReturn(INDEX_NAME_FORMAT);
        when(applicationParams.getCasesIndexType()).thenReturn(INDEX_TYPE);
    }

    @Test
    public void searchShouldMapElasticSearchResultSearchResult() throws IOException {
        ArgumentCaptor<Search> arg = ArgumentCaptor.forClass(Search.class);
        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.isSucceeded()).thenReturn(true);
        when(searchResult.getTotal()).thenReturn(1L);
        when(searchResult.getSourceAsStringList()).thenReturn(newArrayList(caseDetailsElastic));
        when(objectMapper.readValue(caseDetailsElastic, ElasticSearchCaseDetailsDTO.class))
                .thenReturn(caseDetailsDTO);
        when(mapper.dtosToCaseDetailsList(newArrayList(caseDetailsDTO))).thenReturn(newArrayList(caseDetails));
        when(jestClient.execute(any(Search.class))).thenReturn(searchResult);

        CaseDetailsSearchResult caseDetailsSearchResult = searchOperation.execute(CASE_TYPES_ID, "{query}");

        verify(jestClient).execute(arg.capture());

        Search searchRequest = arg.getValue();
        assertThat(searchRequest.getIndex(), equalTo(indices(CASE_TYPES_ID)));
        assertThat(searchRequest.getType(), equalTo(INDEX_TYPE));
        assertThat(caseDetailsSearchResult.getCaseDetails(), equalTo(newArrayList(caseDetails)));
        assertThat(caseDetailsSearchResult.getTotal(), equalTo(1L));
    }

    @Test
    public void searchShouldReturnBadSearchRequestOnFailure() throws IOException {
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