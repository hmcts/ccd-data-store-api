package uk.gov.hmcts.ccd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.test.ElasticsearchTestHelper;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDEX_TYPE;
import static uk.gov.hmcts.ccd.ElasticsearchITConfiguration.INDICES;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_1;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_2;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_2_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_3;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.ADDRESS_LINE_3_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_PRIVATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_RESTRICTED;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST1_SOLICITOR;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST2_PUBLIC;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST_1;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.AUTOTEST_2;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASEWORKER_CAA;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_A;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_B;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_C;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CASE_TYPE_D;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COLLECTION_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_FIXED_LIST_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_NESTED_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COMPLEX_TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_NESTED_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTRY_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTY_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.COUNTY_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CREATED_DATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.CREATED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_TIME_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DEFAULT_CASE_REFERENCE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.DOCUMENT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.EMAIL_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_ALIAS;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_LIST_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.FIXED_RADIO_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.HISTORY_COMPONENT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.IN_PROGRESS_STATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.LAST_MODIFIED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.LAST_STATE_MODIFIED_DATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.MULTI_SELECT_LIST_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_COLLECTION_TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_NUMBER_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NESTED_NUMBER_FIELD_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NUMBER_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.NUMBER_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PARTIAL_PHONE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.PHONE_VALUE_WITH_SPACE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.POST_CODE_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.POST_CODE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STATE_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.STREET_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_ALIAS;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_AREA_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_AREA_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TEXT_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TOWN_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.TOWN_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.VALUE_SUFFIX;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_FIELD;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.YES_OR_NO_VALUE;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.alias;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.caseData;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.caseTypesParam;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.createPostRequest;

@RunWith(Enclosed.class)
public class ElasticsearchIT extends ElasticsearchBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIT.class);

    private static final String DATA_DIR = "elasticsearch/data";

    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Inject
    private ApplicationParams applicationParams;

    private MockMvc mockMvc;

    @BeforeAll
    public static void initElastic(@Autowired EmbeddedElastic embeddedElastic)
        throws IOException, InterruptedException {

        LOG.info("Starting Elastic search...");
        embeddedElastic.start();
        LOG.info("Elastic search started.");
        initData(embeddedElastic);
    }

    private static void initData(EmbeddedElastic embeddedElastic) throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        for (String idx : INDICES) {
            Resource[] resources =
                resourceResolver.getResources(String.format("classpath:%s/%s/*.json", DATA_DIR, idx));
            for (Resource resource : resources) {
                String caseString = IOUtils.toString(resource.getInputStream(), UTF_8);
                embeddedElastic.index(idx, INDEX_TYPE, caseString);
            }
        }
    }

    @AfterAll
    public static void tearDownElastic(@Autowired EmbeddedElastic embeddedElastic) {
        LOG.info("Stopping Elastic search");
        embeddedElastic.stop();
        LOG.info("Elastic search stopped.");
    }

    @Nested
    class UICaseSearchControllerIT {

        private static final String POST_SEARCH_CASES = "/internal/searchCases";
        private static final String CASE_FIELD_ID = "caseFieldId";
        private static final String ERROR_MESSAGE = "message";

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);

            doReturn(authentication).when(securityContext).getAuthentication();
            SecurityContextHolder.setContext(securityContext);

            MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        @Test
        void shouldReturnAllCaseDetailsForDefaultUseCase() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
                () -> assertExampleCaseData(caseDetails.getFields(), false),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }

        @Test
        void shouldReturnAllCaseDetailsForPhoneValueWithSpace() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE_WITH_SPACE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
                () -> assertExampleCaseData(caseDetails.getFields(), false),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }

        @Test
        void shouldNotReturnCaseDetailsForPartialPhoneValue() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PARTIAL_PHONE_VALUE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            assertThat(caseSearchResultViewResource.getCases().size(),is(0));
        }

        @Test
        void shouldReturnAllCaseDetailsForDefaultUseCaseWithRoleCaseworkerCaa() throws Exception {
            MockUtils.setSecurityAuthorities(authentication, CASEWORKER_CAA);
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), ElasticsearchTestHelper.COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
                () -> assertExampleCaseData(caseDetails.getFields(), false),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }

        @Test
        void shouldReturnAllHeaderInfoForDefaultUseCase() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertDefaultUseCaseHeaders(caseSearchResultViewResource.getHeaders())
            );
        }

        @Test
        void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUserRoleColumnIsPopulated() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "TEST");
            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertUseCaseHeadersUserRole(caseSearchResultViewResource.getHeaders()),
                () -> assertExampleCaseDataForUserRole(caseDetails.getFields()),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }

        @Test
        void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUserHasSomeAuthorisationOnCaseFields() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource =
                executeRequest(searchRequest, CASE_TYPE_A, "RDM-8782");
            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);

            List<String> expectedFields = Collections.singletonList(EMAIL_FIELD);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().size(), is(1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getJurisdiction(),
                    is(AUTOTEST_1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getCaseTypeId(),
                    is(CASE_TYPE_A)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().size(), is(1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().get(0),
                    is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(1)),
                () -> expectedFields.forEach(f -> assertThat(caseSearchResultViewResource.getHeaders().get(0)
                    .getFields(), hasItem(hasProperty(CASE_FIELD_ID, is(f))))),

                () -> assertThat(caseDetails.getFields().get(EMAIL_FIELD), is(EMAIL_VALUE)),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }


        @Test
        void shouldReturnAllHeaderInfoForDefaultUseCaseWhenUseHaveNoAuthorisationOnCaseField() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "RDM-8782NOACCESS");
            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getHeaders().size(), is(1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getJurisdiction(),
                    is(AUTOTEST_1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getMetadata().getCaseTypeId(),
                    is(CASE_TYPE_A)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().size(), is(1)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getCases().get(0),
                    is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(0)),
                () -> assertThat(caseDetails.getFields().size(), is(8)),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false)
            );
        }

        @Test
        void shouldReturnAllHeaderInfoForSpecifiedUseCase() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertOrgCasesUseCaseHeaders(caseSearchResultViewResource.getHeaders())
            );
        }

        @Test
        void shouldReturnAllFormattedCaseDetails() throws Exception {
            ElasticsearchTestRequest searchRequest = caseReferenceRequest(DEFAULT_CASE_REFERENCE);

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "ORGCASES");

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().size(), is(1)),
                () -> assertExampleCaseData(caseDetails.getFields(), false),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
                () -> assertExampleCaseData(caseDetails.getFieldsFormatted(), true),
                () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), true)
            );
        }

        @Test
        void shouldOnlyReturnSpecifiedFieldsInResponse() throws Exception {
            String nestedFieldId = COMPLEX_FIELD + "." + COMPLEX_NESTED_FIELD + "." + NESTED_NUMBER_FIELD;
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .source(caseData(TEXT_FIELD))
                .source(caseData(nestedFieldId))
                .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
                .source("INVALID")
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                    is(TEXT_FIELD)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                    is(nestedFieldId)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                    is(MetaData.CaseField.CASE_REFERENCE.getReference())),
                () -> assertThat(caseDetails.getFields().size(), is(11)),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
                () -> assertThat(caseDetails.getFields().get(TEXT_FIELD), is(TEXT_VALUE)),
                () -> assertThat(caseDetails.getFields().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(caseDetails.getFields().containsKey(COMPLEX_FIELD), is(true)),
                () -> assertThat(caseDetails.getFieldsFormatted().size(), is(11)),
                () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), false),
                () -> assertThat(caseDetails.getFieldsFormatted().get(TEXT_FIELD), is(TEXT_VALUE)),
                () -> assertThat(caseDetails.getFieldsFormatted().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(caseDetails.getFieldsFormatted().containsKey(COMPLEX_FIELD), is(true))
            );
        }

        @Test
        void shouldTreatUseCaseRequestWithSourceAsStandardRequest() throws Exception {
            String nestedFieldId = COMPLEX_FIELD + "." + COMPLEX_NESTED_FIELD + "." + NESTED_NUMBER_FIELD;
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .source(caseData(TEXT_FIELD))
                .source(caseData(nestedFieldId))
                .source(MetaData.CaseField.CASE_REFERENCE.getDbColumnName())
                .source("INVALID")
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "SEARCH");

            SearchResultViewItem caseDetails = caseSearchResultViewResource.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(3)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(0).getCaseFieldId(),
                    is(TEXT_FIELD)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(1).getCaseFieldId(),
                    is(nestedFieldId)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().get(2).getCaseFieldId(),
                    is(MetaData.CaseField.CASE_REFERENCE.getReference())),
                () -> assertThat(caseDetails.getFields().size(), is(11)),
                () -> assertExampleCaseMetadata(caseDetails.getFields(), false),
                () -> assertThat(caseDetails.getFields().get(TEXT_FIELD), is(TEXT_VALUE)),
                () -> assertThat(caseDetails.getFields().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(caseDetails.getFields().containsKey(COMPLEX_FIELD), is(true)),
                () -> assertThat(caseDetails.getFieldsFormatted().size(), is(11)),
                () -> assertExampleCaseMetadata(caseDetails.getFieldsFormatted(), false),
                () -> assertThat(caseDetails.getFieldsFormatted().get(TEXT_FIELD), is(TEXT_VALUE)),
                () -> assertThat(caseDetails.getFieldsFormatted().get(nestedFieldId), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(caseDetails.getFieldsFormatted().containsKey(COMPLEX_FIELD), is(true))
            );
        }

        @Test
        void shouldReturnRequestedSupplementaryDataForUseCaseRequest() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .supplementaryData(Arrays.asList("SDField2", "SDField3"))
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getHeaders().get(0).getFields().size(), is(10)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getFields().size(), is(16)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(),
                    is(2)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldReturnAllSupplementaryDataWhenWildcardIsUsed() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .supplementaryData(Collections.singletonList("*"))
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(),
                    is(3)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField1")
                    .asText(), is("SDField1Value")),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldReturnNoSupplementaryDataWhenNotRequestedForUseCaseRequest() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                "orgcases");

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData(), is(nullValue()))
            );
        }

        @Test
        void shouldReturnAllSupplementaryDataByDefaultForStandardRequest() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .build();

            CaseSearchResultViewResource caseSearchResultViewResource = executeRequest(searchRequest, CASE_TYPE_A,
                null);

            assertAll(
                () -> assertThat(caseSearchResultViewResource.getTotal(), is(1L)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().size(),
                    is(3)),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField1")
                    .asText(), is("SDField1Value")),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField2")
                    .asText(), is("SDField2Value")),
                () -> assertThat(caseSearchResultViewResource.getCases().get(0).getSupplementaryData().get("SDField3")
                    .asText(), is("SDField3Value"))
            );
        }

        @Test
        void shouldReturnErrorWithUnsupportedUseCase() throws Exception {
            ElasticsearchTestRequest searchRequest = matchAllRequest();

            JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, "INVALID",
                400);

            assertAll(
                () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                    is("The provided use case 'INVALID' is unsupported for case type 'AAT'."))
            );
        }

        @Test
        void shouldReturnErrorWithMissingQueryField() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder().build();

            JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, null,
                400);

            assertAll(
                () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                    is("missing required field 'query'"))
            );
        }

        @Test
        void shouldReturnErrorWhenElasticsearchErrors() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .sort("invalid.keyword")
                .build();

            JsonNode exceptionNode = executeErrorRequest(searchRequest, CASE_TYPE_A, null, 400);

            assertAll(
                () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                    containsString("No mapping found for [invalid.keyword] in order to sort on"))
            );
        }

        @Test
        void shouldReturnErrorWhenInvalidJsonIsSent() throws Exception {
            ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                .build();

            MockHttpServletRequestBuilder postRequest = post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest.toString().replaceAll("s", "\":,}{}"))
                .param("ctid", CASE_TYPE_A)
                .param("use_case", "orgcases");

            JsonNode exceptionNode = ElasticsearchTestHelper.executeRequest(postRequest, 400,
                mapper, mockMvc, JsonNode.class);

            assertAll(
                () -> assertThat(exceptionNode.get(ERROR_MESSAGE).asText(),
                    containsString("Request requires correctly formatted JSON"))
            );
        }

        private void assertDefaultUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
            List<String> expectedFields = Arrays.asList(HISTORY_COMPONENT_FIELD, FIXED_RADIO_LIST_FIELD,
                DOCUMENT_FIELD, ADDRESS_FIELD, COMPLEX_FIELD,
                COLLECTION_FIELD, MULTI_SELECT_LIST_FIELD, FIXED_LIST_FIELD, TEXT_AREA_FIELD, DATE_TIME_FIELD,
                DATE_FIELD, EMAIL_FIELD, PHONE_FIELD, YES_OR_NO_FIELD, NUMBER_FIELD, TEXT_FIELD,
                MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference(), MetaData.CaseField.LAST_MODIFIED_DATE
                    .getReference(),
                MetaData.CaseField.CREATED_DATE.getReference(), MetaData.CaseField.JURISDICTION.getReference(),
                MetaData.CaseField.CASE_TYPE.getReference(), MetaData.CaseField.SECURITY_CLASSIFICATION.getReference(),
                MetaData.CaseField.CASE_REFERENCE.getReference(), MetaData.CaseField.STATE.getReference());

            assertAll(
                () -> assertThat(headers.size(), is(1)),
                () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
                () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
                () -> assertThat(headers.get(0).getCases().size(), is(1)),
                () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(headers.get(0).getFields().size(), is(24)),
                () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(),
                    hasItem(hasProperty(CASE_FIELD_ID,
                        is(f)))))
            );
        }

        private void assertUseCaseHeadersUserRole(List<SearchResultViewHeaderGroup> headers) {
            List<String> expectedFields = Arrays.asList(COLLECTION_FIELD, EMAIL_FIELD, FIXED_LIST_FIELD,
                TEXT_FIELD, MetaData.CaseField.STATE.getReference());

            assertAll(
                () -> assertThat(headers.size(), is(1)),
                () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
                () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
                () -> assertThat(headers.get(0).getCases().size(), is(1)),
                () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(headers.get(0).getFields().size(), is(5)),
                () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(),
                    hasItem(hasProperty(CASE_FIELD_ID,
                        is(f)))))
            );
        }

        private void assertOrgCasesUseCaseHeaders(List<SearchResultViewHeaderGroup> headers) {
            List<String> expectedFields = Arrays.asList(TEXT_FIELD, EMAIL_FIELD, FIXED_LIST_FIELD, COLLECTION_FIELD,
                COMPLEX_FIELD, DATE_FIELD,
                DATE_TIME_FIELD, COMPLEX_FIELD + ".ComplexTextField", MetaData.CaseField.CREATED_DATE.getReference(),
                MetaData.CaseField.STATE.getReference());

            assertAll(
                () -> assertThat(headers.size(), is(1)),
                () -> assertThat(headers.get(0).getMetadata().getJurisdiction(), is(AUTOTEST_1)),
                () -> assertThat(headers.get(0).getMetadata().getCaseTypeId(), is(CASE_TYPE_A)),
                () -> assertThat(headers.get(0).getCases().size(), is(1)),
                () -> assertThat(headers.get(0).getCases().get(0), is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(headers.get(0).getFields().size(), is(10)),
                () -> expectedFields.forEach(f -> assertThat(headers.get(0).getFields(),
                    hasItem(hasProperty(CASE_FIELD_ID,
                        is(f)))))
            );
        }

        private void assertExampleCaseData(Map<String, Object> data, boolean formatted) {
            assertAll(
                () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(0).get(VALUE), is(COLLECTION_VALUE)),
                () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(1).get(VALUE),
                    is("CollectionTextValue1")),
                () -> assertThat(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_FIXED_LIST_FIELD), is("VALUE3")),
                () -> assertThat(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD))
                    .get(NESTED_NUMBER_FIELD), is(NESTED_NUMBER_FIELD_VALUE)),
                () -> assertThat(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_TEXT_FIELD), is(COMPLEX_TEXT_VALUE)),
                () -> assertThat(asCollection(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD))
                    .get(NESTED_COLLECTION_TEXT_FIELD)).get(0).get(VALUE), is("NestedCollectionTextValue1")),
                () -> assertThat(asCollection(asMap(asMap(data.get(COMPLEX_FIELD)).get(COMPLEX_NESTED_FIELD))
                    .get(NESTED_COLLECTION_TEXT_FIELD)).get(1).get(VALUE), is("NestedCollectionTextValue2")),
                () -> assertThat(data.get(DATE_FIELD), is(formatted ? "12/2007" : DATE_VALUE)),
                () -> assertThat(data.get(DATE_TIME_FIELD),
                    is(formatted ? "Saturday, 1 February 2003" : DATE_TIME_VALUE)),
                () -> assertThat(data.get(EMAIL_FIELD), is(EMAIL_VALUE)),
                () -> assertThat(data.get(FIXED_LIST_FIELD), is(FIXED_LIST_VALUE)),
                () -> assertThat(data.get(FIXED_RADIO_LIST_FIELD), is(nullValue())),
                () -> assertThat(data.get(TEXT_FIELD), is(TEXT_VALUE))
            );
        }

        private void assertExampleCaseDataForUserRole(Map<String, Object> data) {
            assertAll(
                () -> assertThat(data.get(EMAIL_FIELD), is(EMAIL_VALUE)),
                () -> assertThat(data.get(FIXED_LIST_FIELD), is(FIXED_LIST_VALUE)),
                () -> assertThat(data.get(TEXT_FIELD), is(TEXT_VALUE)),
                () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(0).get(VALUE), is(COLLECTION_VALUE)),
                () -> assertThat(asCollection(data.get(COLLECTION_FIELD)).get(1).get(VALUE),
                    is("CollectionTextValue1"))
            );
        }

        private void assertExampleCaseMetadata(Map<String, Object> data, boolean formatted) {
            assertAll(
                () -> assertThat(data.get(MetaData.CaseField.JURISDICTION.getReference()), is(AUTOTEST_1)),
                () -> assertThat(data.get(MetaData.CaseField.CASE_TYPE.getReference()), is(CASE_TYPE_A)),
                () -> assertThat(data.get(MetaData.CaseField.CREATED_DATE.getReference()),
                    is(formatted ? "07 05 2020" : CREATED_DATE_VALUE)),
                () -> assertThat(data.get(MetaData.CaseField.LAST_MODIFIED_DATE.getReference()),
                    is(LAST_MODIFIED_DATE_VALUE)),
                () -> assertThat(data.get(MetaData.CaseField.LAST_STATE_MODIFIED_DATE.getReference()),
                    is(LAST_STATE_MODIFIED_DATE_VALUE)),
                () -> assertThat(data.get(MetaData.CaseField.CASE_REFERENCE.getReference()),
                    is(DEFAULT_CASE_REFERENCE)),
                () -> assertThat(data.get(MetaData.CaseField.STATE.getReference()), is(STATE_VALUE)),
                () -> assertThat(data.get(MetaData.CaseField.SECURITY_CLASSIFICATION.getReference()),
                    is(SecurityClassification.PUBLIC.name()))
            );
        }

        private Map<String, Object> asMap(Object obj) {
            return (Map<String, Object>) obj;
        }

        private List<Map<String, Object>> asCollection(Object obj) {
            return (List<Map<String, Object>>) obj;
        }

        private CaseSearchResultViewResource executeRequest(ElasticsearchTestRequest searchRequest,
                                                            String caseTypeParam, String useCase) throws Exception {
            MockHttpServletRequestBuilder postRequest =
                createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, useCase);

            return ElasticsearchTestHelper.executeRequest(postRequest, 200, mapper, mockMvc,
                CaseSearchResultViewResource.class);
        }

        private JsonNode executeErrorRequest(ElasticsearchTestRequest searchRequest,
                                             String caseTypeParam,
                                             String useCase,
                                             int expectedErrorCode) throws Exception {
            MockHttpServletRequestBuilder postRequest = createPostRequest(POST_SEARCH_CASES, searchRequest,
                caseTypeParam, useCase);

            return ElasticsearchTestHelper.executeRequest(postRequest, expectedErrorCode, mapper, mockMvc,
                JsonNode.class);
        }
    }

    @AutoConfigureWireMock(port = 0)
    @ActiveProfiles("test")
    @RunWith(SpringRunner.class)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(locations = "classpath:test.properties")
    @Nested
    class CaseSearchEndpointESSecurityIT {

        private static final String POST_SEARCH_CASES = "/searchCases";
        private static final String SECURITY_CASE_2 = "1589460125872336";

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);

            doReturn(authentication).when(securityContext).getAuthentication();
            SecurityContextHolder.setContext(securityContext);

            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }

        @Nested
        class CrudTest {

            // AuthorisationCaseField

            @Test
            void shouldReturnCaseFieldsWithCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.containsKey(EMAIL_FIELD), is(true))
                );
            }

            @Test
            void shouldReturnCaseFieldsWithCrudWithoutRead() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.containsKey(YES_OR_NO_FIELD), is(false))
                );
            }

            @Test
            void shouldNotReturnCaseFieldsWithoutCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PRIVATE);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertThat(data.containsKey(YES_OR_NO_FIELD), is(false))
                );
            }

            // AuthorisationCaseState

            @Test
            void shouldReturnCasesWithStateCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(STATE, IN_PROGRESS_STATE))
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getReference(), is(1589460099608690L))
                );
            }

            @Test
            void shouldNotReturnCasesWithStateCrudWithoutRead() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(STATE, IN_PROGRESS_STATE))
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(0L))
                );
            }

            @Test
            void shouldNotReturnCasesWithoutStateCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(STATE, IN_PROGRESS_STATE))
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PRIVATE);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(0L))
                );
            }

            // AuthorisationCaseType

            @Test
            void shouldReturnCaseTypesWithCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal() > 0L, is(true))
                );
            }

            @Test
            void shouldNotReturnCaseTypesWithCrudWithoutRead() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A, AUTOTEST1_PRIVATE);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(0L))
                );
            }

            @Test
            void shouldNotReturnCaseTypesWithoutCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A, AUTOTEST1_RESTRICTED);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(0L))
                );
            }

            // AuthorisationComplexType

            @Test
            void shouldReturnComplexNestedFieldsWithCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                assertAll(
                    () -> assertThat(getFirstCaseData(caseSearchResult).get(COMPLEX_FIELD).has(COMPLEX_TEXT_FIELD),
                        is(true))
                );
            }

            @Test
            void shouldNotReturnComplexNestedFieldsWithCrudWithoutRead() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

                assertAll(
                    () -> assertThat(getFirstCaseData(caseSearchResult).get(COMPLEX_FIELD).has(COMPLEX_TEXT_FIELD),
                        is(false))
                );
            }

            @Test
            void shouldReturnComplexNestedFieldsWithoutCrud() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PRIVATE);

                assertAll(
                    () -> assertThat(getFirstCaseData(caseSearchResult).get(COMPLEX_FIELD).has(COMPLEX_TEXT_FIELD),
                        is(true))
                );
            }
        }

        @Nested
        class SecurityClassificationTest {

            // Case

            @Test
            void shouldReturnCasesWithLowerCaseSC() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(STATE, STATE_VALUE))
                    .sort(CREATED_DATE)
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getSecurityClassification(),
                        is(SecurityClassification.PRIVATE)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getSecurityClassification(),
                        is(SecurityClassification.PUBLIC))
                );
            }

            @Test
            void shouldNotReturnCasesWithHigherCaseSC() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getSecurityClassification(),
                        is(SecurityClassification.PUBLIC))
                );
            }

            // CaseField

            @Test
            void shouldReturnCaseFieldsWithLowerSC() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.containsKey(MULTI_SELECT_LIST_FIELD), is(true)), // RESTRICTED
                    () -> assertThat(data.containsKey(PHONE_FIELD), is(true)), // PRIVATE
                    () -> assertThat(data.containsKey(DATE_FIELD), is(true)) // PUBLIC
                );
            }

            @Test
            void shouldNotReturnCaseFieldsWithHigherSC() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.containsKey(MULTI_SELECT_LIST_FIELD), is(false)), // RESTRICTED
                    () -> assertThat(data.containsKey(PHONE_FIELD), is(false)), // PRIVATE
                    () -> assertThat(data.containsKey(COLLECTION_FIELD), is(true)) // PUBLIC
                );
            }

            // CaseType

            @Test
            void shouldReturnCasesWithLowerCaseTypeSC() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_C, CASE_TYPE_D), AUTOTEST1_RESTRICTED);

                assertAll(
                    () -> assertThat(caseSearchResult.getCases().get(0).getCaseTypeId(), is(CASE_TYPE_C)), // PUBLIC
                    () -> assertThat(caseSearchResult.getCases().get(3).getCaseTypeId(), is(CASE_TYPE_D)) // RESTRICTED
                );
            }

            @Test
            void shouldNotReturnCasesWithHigherCaseTypeSC() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_C, CASE_TYPE_D), AUTOTEST1_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getCaseTypeId(), is(CASE_TYPE_C)) // PUBLIC
                );
            }

            // ComplexTypes

            @Test
            void shouldReturnComplexNestedFieldsWithLowerSC() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD)
                        .has(NESTED_COLLECTION_TEXT_FIELD), is(true)), // RESTRICTED
                    () -> assertThat(data.get(ADDRESS_FIELD).has(POST_CODE_FIELD), is(true)), // PRIVATE
                    () -> assertThat(data.get(ADDRESS_FIELD).has(ADDRESS_LINE_1), is(true)) // PUBLIC
                );
            }

            @Test
            void shouldNotReturnComplexNestedFieldsWithHigherSC() throws Exception {
                ElasticsearchTestRequest searchRequest = caseReferenceRequest(SECURITY_CASE_2);

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                assertAll(
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD)
                        .has(NESTED_COLLECTION_TEXT_FIELD), is(false)), // RESTRICTED
                    () -> assertThat(data.get(COMPLEX_FIELD).has(POST_CODE_FIELD), is(false)), // PRIVATE
                    () -> assertThat(data.get(COMPLEX_FIELD).has(COMPLEX_TEXT_FIELD), is(true)) // PUBLIC
                );
            }
        }

        @Nested
        class GeneralAccessTest {

            @Test
            void shouldOnlyReturnCasesFromCaseTypesWithJurisdictionRole() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B, CASE_TYPE_C),
                        AUTOTEST2_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getCaseTypeId(), is(CASE_TYPE_B))
                );
            }

            @Test
            void shouldMergePermissionsOfMultipleRolesForCaseData() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .sort(CREATED_DATE)
                    .build();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_PUBLIC, AUTOTEST1_PRIVATE,
                        AUTOTEST1_RESTRICTED);

                Map<String, JsonNode> data = getFirstCaseData(caseSearchResult);
                // Comments for assertions below describe some example scenarios for which a given role would usually
                // NOT allow data/cases to be returned if a user conducting the search ONLY had that role - expressed in
                // the form "<Scenario> (<role>)"
                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getSecurityClassification(),
                        is(SecurityClassification.PRIVATE)), // Case SC (caseworker-autotest1)
                    () -> assertThat(caseSearchResult.getCases().get(2).getState(),
                        is(IN_PROGRESS_STATE)), // State CRUD (caseworker-autotest1)
                    () -> assertThat(data.get(COMPLEX_FIELD).has(COMPLEX_TEXT_FIELD),
                        is(true)), // Complex nested field CRUD (caseworker-autotest1-restricted)
                    // Complex nested field SC (caseworker-autotest1)
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD)
                        .has(NESTED_COLLECTION_TEXT_FIELD), is(true)),
                    // Case field SC (caseworker-autotest1) & Case field CRUD (caseworker-autotest1-private)
                    () -> assertThat(data.containsKey(MULTI_SELECT_LIST_FIELD),
                        is(true))
                );
            }

            @Test
            void shouldMergePermissionsOfMultipleRolesForCases() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .sort(CREATED_DATE)
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, caseTypesParam(CASE_TYPE_B,
                    CASE_TYPE_C),
                    AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getJurisdiction(), is(AUTOTEST_2)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getCaseTypeId(), is(CASE_TYPE_B)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getJurisdiction(), is(AUTOTEST_1)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getCaseTypeId(), is(CASE_TYPE_C))
                );
            }
        }

        /*
        The following tests require the Spring @SQL annotation, which does not work in @Nested classes (see SPR-15366)
         To use @SQL annotation in @Nested classes is to copy the annotations from the enclosing test class to the
         nested test class. reason you have to duplicate the configuration is that annotations in Spring are not
         inherited from enclosing classes. This is a known limitation of the Spring TestContext Framework
        */
        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_elasticsearch_cases.sql"})
        void shouldOnlyReturnCasesSolicitorHasBeenGrantedAccessTo() throws Exception {
            ElasticsearchTestRequest searchRequest = matchAllRequest();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_SOLICITOR);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                () -> Assertions.assertThat(caseSearchResult.getCases()).extracting("reference")
                    .contains(1589460125872336L, 1589460099608691L)
            );
        }

        @Test
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_elasticsearch_cases.sql"})
        void shouldReturnAllCasesForCaseworker() throws Exception {
            ElasticsearchTestRequest searchRequest = matchAllRequest();

            CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_C, AUTOTEST1_RESTRICTED);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(4L))
            );
        }

        private CaseSearchResult executeRequest(ElasticsearchTestRequest searchRequest,
                                                String caseTypeParam, String... roles) throws Exception {
            MockUtils.setSecurityAuthorities(authentication, roles);
            MockHttpServletRequestBuilder postRequest = createPostRequest(POST_SEARCH_CASES, searchRequest,
                caseTypeParam, null);

            return ElasticsearchTestHelper.executeRequest(postRequest, 200, mapper, mockMvc,
                CaseSearchResult.class);
        }

        private Map<String, JsonNode> getFirstCaseData(CaseSearchResult caseSearchResult) {
            return caseSearchResult.getCases().get(0).getData();
        }
    }

    @Nested
    class CaseSearchEndpointESIT {

        private static final String POST_SEARCH_CASES = "/searchCases";

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);

            doReturn(authentication).when(securityContext).getAuthentication();
            SecurityContextHolder.setContext(securityContext);

            MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);

            mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        }

        @Nested
        class CrossCaseTypeSearch {

            // Note that cross case type searches do NOT return case data
            @Test
            void shouldReturnAllCasesForAllSpecifiedCaseTypes() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                    () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_A).size(), is(2)),
                    () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_B).size(), is(1)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(0)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(0)),
                    () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(0))
                );
            }

            @Test
            void shouldReturnRequestedAliasSource() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .source(alias(TEXT_ALIAS))
                    .sort(CREATED_DATE)
                    .build();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                    () -> assertThat(caseSearchResult.getCases().get(0).getData().get(TEXT_ALIAS).asText(),
                        is(TEXT_VALUE)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getData().get(TEXT_ALIAS).asText(),
                        is("CCC TextValue")),
                    () -> assertThat(caseSearchResult.getCases().get(2).getData().get(TEXT_ALIAS).asText(),
                        is("BBB TextValue")),
                    () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(1)),
                    () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(1)),
                    () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(1))
                );
            }

            @Test // Note that the size and sort is applied to each separate case type then results combined
            void shouldReturnPaginatedResults() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchAllQuery())
                    .sort(CREATED_DATE)
                    .size(1)
                    .build();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                    () -> assertThat(caseSearchResult.getCases().size(), is(2)) // = Size * Number of case types
                );
            }

            @Test
            void shouldQueryOnAliasField() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(alias(FIXED_LIST_ALIAS), FIXED_LIST_VALUE))
                    .build();

                CaseSearchResult caseSearchResult =
                    executeRequest(searchRequest, caseTypesParam(CASE_TYPE_A, CASE_TYPE_B));

                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                    () -> Assertions.assertThat(caseSearchResult.getCases()).extracting("reference")
                        .contains(1588866820969121L, 1588870615652827L)
                );
            }
        }

        @Nested
        class SingleCaseTypeSearch {

            @Test
            void shouldReturnAllCaseDetails() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(boolQuery()
                        .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                        .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                        .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                        .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                        .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                        .must(matchQuery(caseData(COUNTRY_FIELD), COUNTRY_VALUE)) // Complex
                        .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                        .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

                CaseDetails caseDetails = caseSearchResult.getCases().get(0);
                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertExampleCaseMetadata(caseDetails),
                    () -> assertExampleCaseData(caseDetails),
                    () -> assertThat(caseDetails.getSupplementaryData().size(), is(3)),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField1").asText(),
                        is("SDField1Value")),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(),
                        is("SDField2Value")),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(),
                        is("SDField3Value"))
                );
            }

            @Test
            void shouldReturnRequestedSupplementaryData() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                    .supplementaryData(Arrays.asList("SDField2", "SDField3"))
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

                CaseDetails caseDetails = caseSearchResult.getCases().get(0);
                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertExampleCaseMetadata(caseDetails),
                    () -> assertExampleCaseData(caseDetails),
                    () -> assertThat(caseDetails.getSupplementaryData().size(), is(2)),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(),
                        is("SDField2Value")),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(),
                        is("SDField3Value"))
                );
            }

            @Test
            void shouldReturnAllSupplementaryDataWhenWildcardIsUsed() throws Exception {
                ElasticsearchTestRequest searchRequest = ElasticsearchTestRequest.builder()
                    .query(matchQuery(MetaData.CaseField.CASE_REFERENCE.getDbColumnName(), DEFAULT_CASE_REFERENCE))
                    .supplementaryData(Collections.singletonList("*"))
                    .build();

                CaseSearchResult caseSearchResult = executeRequest(searchRequest, CASE_TYPE_A);

                CaseDetails caseDetails = caseSearchResult.getCases().get(0);
                assertAll(
                    () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                    () -> assertExampleCaseMetadata(caseDetails),
                    () -> assertExampleCaseData(caseDetails),
                    () -> assertThat(caseDetails.getSupplementaryData().size(), is(3)),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField1").asText(),
                        is("SDField1Value")),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField2").asText(),
                        is("SDField2Value")),
                    () -> assertThat(caseDetails.getSupplementaryData().get("SDField3").asText(),
                        is("SDField3Value"))
                );
            }

            @Test
            void shouldErrorWhenInvalidCaseTypeIsProvided() throws Exception {
                ElasticsearchTestRequest searchRequest = matchAllRequest();

                JsonNode exceptionNode = executeErrorRequest(searchRequest, "INVALID", 404);

                assertAll(
                    () -> assertThat(exceptionNode.get("message").asText(),
                        startsWith("Resource not found when getting case type definition for INVALID"))
                );
            }

            public void assertExampleCaseMetadata(CaseDetails caseDetails) {
                assertAll(
                    () -> assertThat(caseDetails.getJurisdiction(), is("AUTOTEST1")),
                    () -> assertThat(caseDetails.getCaseTypeId(), is(CASE_TYPE_A)),
                    () -> assertThat(caseDetails.getCreatedDate().toString(), is(CREATED_DATE_VALUE)),
                    () -> assertThat(caseDetails.getLastModified().toString(), is(LAST_MODIFIED_DATE_VALUE)),
                    () -> assertThat(caseDetails.getLastStateModifiedDate().toString(),
                        is(LAST_STATE_MODIFIED_DATE_VALUE)),
                    () -> assertThat(caseDetails.getReference(), is(1588866820969121L)),
                    () -> assertThat(caseDetails.getState(), is(STATE_VALUE)),
                    () -> assertThat(caseDetails.getSecurityClassification(), is(SecurityClassification.PUBLIC))
                );
            }

            public void assertExampleCaseData(CaseDetails caseDetails) {
                Map<String, JsonNode> data = caseDetails.getData();
                assertAll(
                    () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_1).asText(), is(STREET_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_2).asText(), is(ADDRESS_LINE_2_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(ADDRESS_LINE_3).asText(), is(ADDRESS_LINE_3_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(COUNTRY_NESTED_FIELD).asText(), is(COUNTRY_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(COUNTY_FIELD).asText(), is(COUNTY_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(POST_CODE_FIELD).asText(), is(POST_CODE_VALUE)),
                    () -> assertThat(data.get(ADDRESS_FIELD).get(TOWN_FIELD).asText(), is(TOWN_VALUE)),
                    () -> assertThat(data.get(COLLECTION_FIELD).toString(),
                        is("[{\"id\":\"2c6da07c-1dfb-4765-88f6-96cd5d5f33b1\",\"value\":\"CollectionTextValue2\"},"
                            + "{\"id\":\"f7d67f03-172d-4adb-85e5-ca958ad442ce\",\"value\":\"CollectionTextValue1\"}]")),
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_TEXT_FIELD).asText(), is(COMPLEX_TEXT_VALUE)),
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_FIXED_LIST_FIELD).asText(),
                        is(COMPLEX_FIXED_LIST_VALUE)),
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD).get(NESTED_NUMBER_FIELD)
                        .asText(), is(NESTED_NUMBER_FIELD_VALUE)),
                    () -> assertThat(data.get(COMPLEX_FIELD).get(COMPLEX_NESTED_FIELD).get(NESTED_COLLECTION_TEXT_FIELD)
                            .toString(),
                        is("[{\"id\":\"8e19ccb3-2d8c-42f0-abe1-fa585cc2d8c8\","
                            + "\"value\":\"NestedCollectionTextValue1\"},"
                            + "{\"id\":\"95f337e8-5f17-4b25-a795-b7f84f4b2855\","
                            + "\"value\":\"NestedCollectionTextValue2\"}]")),
                    () -> assertThat(data.get(DATE_FIELD).asText(), is(DATE_VALUE)),
                    () -> assertThat(data.get(DATE_TIME_FIELD).asText(), is(DATE_TIME_VALUE)),
                    () -> assertThat(data.get(EMAIL_FIELD).asText(), is(EMAIL_VALUE)),
                    () -> assertThat(data.get(FIXED_LIST_FIELD).asText(), is(FIXED_LIST_VALUE)),
                    () -> assertThat(data.get(FIXED_RADIO_LIST_FIELD).isNull(), is(true)),
                    () -> assertThat(data.get(MULTI_SELECT_LIST_FIELD).toString(),
                        is("[\"OPTION2\",\"OPTION4\"]")),
                    () -> assertThat(data.get(NUMBER_FIELD).asText(), is(NUMBER_VALUE)),
                    () -> assertThat(data.get(PHONE_FIELD).asText(), is(PHONE_VALUE)),
                    () -> assertThat(data.get(TEXT_AREA_FIELD).asText(), is(TEXT_AREA_VALUE)),
                    () -> assertThat(data.get(TEXT_FIELD).asText(), is(TEXT_VALUE)),
                    () -> assertThat(data.get(YES_OR_NO_FIELD).asText(), is(YES_OR_NO_VALUE))
                );
            }
        }

        private CaseSearchResult executeRequest(ElasticsearchTestRequest searchRequest, String caseTypeParam)
            throws Exception {
            MockHttpServletRequestBuilder postRequest =
                createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, null);

            return ElasticsearchTestHelper.executeRequest(postRequest, 200, mapper, mockMvc,
                CaseSearchResult.class);
        }

        private JsonNode executeErrorRequest(ElasticsearchTestRequest searchRequest,
                                             String caseTypeParam,
                                             int expectedErrorCode) throws Exception {
            MockHttpServletRequestBuilder postRequest =
                createPostRequest(POST_SEARCH_CASES, searchRequest, caseTypeParam, null);

            return ElasticsearchTestHelper.executeRequest(postRequest, expectedErrorCode, mapper, mockMvc,
                JsonNode.class);
        }
    }
}
